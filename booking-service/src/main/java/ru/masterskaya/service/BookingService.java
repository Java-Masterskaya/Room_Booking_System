package ru.masterskaya.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.masterskaya.dto.BookingRequest;
import ru.masterskaya.exceptions.BookingConflictException;
import ru.masterskaya.model.Booking;
import ru.masterskaya.model.OutboxEvent;
import ru.masterskaya.model.OutboxStatus;
import ru.masterskaya.repository.BookingRepository;
import ru.masterskaya.repository.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.redis.lock.wait-time-seconds}")
    private Long lockWaitTime;

    @Value("${app.redis.lock.lease-time-seconds}")
    private Long lockLeaseTime;

    private final ObjectMapper objectMapper;

    public void createBooking(BookingRequest bookingRequest) {

        String lockKey = "lock:room:" + bookingRequest.roomId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(lockWaitTime, lockLeaseTime, TimeUnit.SECONDS)) {

                Booking savedBooking;
                try {
                    log.debug("Замок Redis успешно захвачен для комнаты {}", bookingRequest.roomId());
                    savedBooking = transactionTemplate.execute(status -> processBookingSave(bookingRequest));

                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("Замок Redis успешно освобожден для комнаты {}.", bookingRequest.roomId());
                    }
                }

                if (savedBooking != null) {
                    createAndSaveOutboxEvent(savedBooking);
                }
            } else {
                throw new BookingConflictException("Комната временно заблокирована из-за высокой нагрузки. Попробуйте позже.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Поток исполнения был прерван во время ожидания лока Redis", exception);
        }
    }

    private Booking processBookingSave(BookingRequest bookingRequest) {

        LocalDateTime startTimeLocal = bookingRequest.startTime().toLocalDateTime();
        LocalDateTime endTimeLocal = bookingRequest.endTime().toLocalDateTime();

        boolean hasOverlap = bookingRepository.existsOverlapping(
                Long.valueOf(bookingRequest.roomId()), startTimeLocal, endTimeLocal);
        if (hasOverlap) {
            throw new BookingConflictException("Конфликт: Выбранное время уже занято другой бронью.");
        }

        Booking booking = new Booking(
                Long.valueOf(bookingRequest.userId()),
                Long.valueOf(bookingRequest.roomId()),
                startTimeLocal,
                endTimeLocal
        );

        try {
            return bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Сработала защита на уровне EXCLUDE индекса PostgreSQL для комнаты {}", bookingRequest.roomId());
            throw new BookingConflictException("Конфликт: выбранное время для этой комнаты уже занято.");
        }
    }

    private void createAndSaveOutboxEvent(Booking savedBooking) {
        Map<String, Object> payload = Map.of(
                "bookingId", savedBooking.getId(),
                "userId", savedBooking.getUserId(),
                "roomId", savedBooking.getRoomId(),
                "startTime", savedBooking.getStartTime().toString(),
                "endTime", savedBooking.getEndTime().toString()
        );
        String jsonPayload = objectMapper.writeValueAsString(payload);

        OutboxEvent outboxEvent = new OutboxEvent(
                OutboxEvent.AGGREGATE_TYPE_BOOKING,
                savedBooking.getId().toString(),
                OutboxEvent.EVENT_BOOKING_CREATED,
                jsonPayload,
                OutboxStatus.NEW
        );
        outboxEventRepository.save(outboxEvent);
    }
}

