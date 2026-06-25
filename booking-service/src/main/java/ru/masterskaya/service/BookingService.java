package ru.masterskaya.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.masterskaya.dto.BookingPayLoad;
import ru.masterskaya.dto.BookingRequest;
import ru.masterskaya.exceptions.BookingConflictException;
import ru.masterskaya.model.Booking;
import ru.masterskaya.model.OutboxEvent;
import ru.masterskaya.model.OutboxStatus;
import ru.masterskaya.repository.BookingRepository;
import ru.masterskaya.repository.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.lock.wait-time-seconds}")
    private Long lockWaitTime;

    public void createBooking(BookingRequest bookingRequest) {

        String lockKey = "lock:room:" + bookingRequest.roomId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(lockWaitTime, TimeUnit.SECONDS)) {
                try {
                    log.debug("Замок Redis успешно захвачен для комнаты {}", bookingRequest.roomId());
                    transactionTemplate.executeWithoutResult(status -> processBookingSave(bookingRequest));
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("Замок Redis успешно освобожден для комнаты {}.", bookingRequest.roomId());
                    }
                }
            } else {
                throw new BookingConflictException("Комната временно заблокирована из-за высокой нагрузки. Попробуйте позже.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Поток исполнения был прерван во время ожидания лока Redis", exception);
        }
    }

    private void processBookingSave(BookingRequest bookingRequest) {
        LocalDateTime startTimeLocal = bookingRequest.startTime()
                .withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        LocalDateTime endTimeLocal = bookingRequest.endTime()
                .withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();

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

        Booking savedBooking;

        try {
            savedBooking = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Сработала защита на уровне EXCLUDE индекса PostgreSQL для комнаты {}", bookingRequest.roomId());
            throw new BookingConflictException("Конфликт: выбранное время для этой комнаты уже занято.");
        }

        OutboxEvent outboxEvent = createOutboxEvent(savedBooking);
        outboxEventRepository.save(outboxEvent);
    }

    private OutboxEvent createOutboxEvent(Booking savedBooking) {

        BookingPayLoad bookingPayLoad = new BookingPayLoad(
                savedBooking.getId(),
                savedBooking.getUserId(),
                savedBooking.getRoomId(),
                savedBooking.getStartTime().toString(),
                savedBooking.getEndTime().toString()
        );

        String jsonPayload = objectMapper.writeValueAsString(bookingPayLoad);

        return new OutboxEvent(
                OutboxEvent.AGGREGATE_TYPE_BOOKING,
                savedBooking.getId().toString(),
                OutboxEvent.EVENT_BOOKING_CREATED,
                jsonPayload,
                OutboxStatus.NEW
        );
    }
}