package ru.masterskaya.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import ru.masterskaya.dto.BookingRequest;
import ru.masterskaya.exceptions.BookingConflictException;
import ru.masterskaya.model.*;
import ru.masterskaya.repository.BookingRepository;
import ru.masterskaya.repository.OutboxEventRepository;
import ru.masterskaya.repository.RoomRepository;
import ru.masterskaya.repository.UserRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@Import(BookingServiceIT.TestKafkaConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BookingServiceIT {

    @TestConfiguration
    static class TestKafkaConfig {
        @Bean
        @Primary
        public KafkaTemplate<String, String> kafkaTemplate() {
            KafkaTemplate<String, String> mock = Mockito.mock(KafkaTemplate.class);

            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            Mockito.when(mock.send(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                    .thenReturn(future);
            return mock;
        }
    }

    @Autowired
    private BookingService bookingService;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private Environment environment;

    private Long testUserId;
    private Long testRoomId;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {

        boolean isIntegration = List.of(environment.getActiveProfiles()).contains("integration");

        if (!isIntegration) {
            throw new IllegalStateException("Запуск тест только под профилем integration");
        }

        User user = User.builder()
                .email("user@email.com")
                .name("userName")
                .password("userPassword")
                .role(Role.USER)
                .build();
        User savedUser = userRepository.save(user);
        testUserId = savedUser.getId();

        Room room = Room.builder()
                .name("room")
                .capacity(2)
                .equipment(List.of("СТУЛ"))
                .build();
        Room savedRoom = roomRepository.save(room);
        testRoomId = savedRoom.getId();
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    void createBooking_ShouldHandleRaceCondition_AndAllowOnlyOneSuccessfulBooking() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        // Один человек отправляет 10 запросов
        BookingRequest concurrentRequest = new BookingRequest(
                testRoomId.intValue(),  // roomId
                testUserId.intValue(), // userId
                OffsetDateTime.parse("2026-06-05T10:00:00Z"),
                OffsetDateTime.parse("2026-06-05T11:00:00Z")
        );

        // Создаем 10 потоков с задачей бронирования комнаты
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();

                    bookingService.createBooking(concurrentRequest);
                    successCount.incrementAndGet(); // Если бронирование прошло успешно

                } catch (BookingConflictException exception) {
                    conflictCount.incrementAndGet(); // Если сработал замок Redis или EXCLUDE индекс Postgres
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await();

        startLatch.countDown();

        finishLatch.await();

        // ASSERTIONS

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(numberOfThreads - 1);
        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.count()).isEqualTo(1);

        // Проверка целостности данных
        List<Booking> bookings = bookingRepository.findAll();
        assertThat(bookings.size()).isEqualTo(1);
        Booking booking = bookings.get(0);
        assertThat(booking.getRoomId()).isEqualTo(concurrentRequest.roomId().longValue());
        assertThat(booking.getUserId()).isEqualTo(concurrentRequest.userId().longValue());
        assertThat(booking.getStartTime())
                .isEqualTo(concurrentRequest.startTime().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        assertThat(booking.getEndTime())
                .isEqualTo(concurrentRequest.endTime().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events.size()).isEqualTo(1);
        OutboxEvent event = events.get(0);
        assertThat(event.getAggregateType()).isEqualTo("BOOKING");
        assertThat(event.getEventType()).isEqualTo("BOOKING_CREATED");
        assertThat(event.getStatus()).isEqualTo("NEW");
    }
}