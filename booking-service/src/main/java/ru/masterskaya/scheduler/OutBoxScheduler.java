package ru.masterskaya.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import ru.masterskaya.model.OutboxEvent;
import ru.masterskaya.model.OutboxStatus;
import ru.masterskaya.repository.OutboxEventRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OutBoxScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    private static final String TOPIC = "booking.reservation.v1";

    @Scheduled(fixedDelay = 1000)
    public void processOutboxEvent() {

        List<OutboxEvent> events = transactionTemplate.execute(status -> lockEventsForProcessing());

        if (events == null || events.isEmpty()) {
            return;
        }

        sendEventsToKafka(events);

        transactionTemplate.executeWithoutResult(status -> updateFinalStatuses(events));

    }

    private List<OutboxEvent> lockEventsForProcessing() {
        List<OutboxEvent> events = outboxEventRepository.findNewEvents(PageRequest.of(0, 50));

        if (events.isEmpty()) {
            return events;
        }
        for (OutboxEvent event : events) {
            event.setStatus(OutboxStatus.PROCESSING);
        }

        return events;
    }

    private void sendEventsToKafka(List<OutboxEvent> events) {
        List<CompletableFuture<SendResult<String, String>>> futures = new ArrayList<>();

        for (OutboxEvent event : events) {
            futures.add(kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayLoad()));
        }

        for (int i = 0; i < futures.size(); i++) {
            OutboxEvent event = events.get(i);
            try {
                futures.get(i).get(5, TimeUnit.SECONDS);
                event.setStatus(OutboxStatus.PROCESSED);
            } catch (Exception exception) {
                log.error("Критический сбой при обработке события {}: {}", event.getId(), exception.getMessage());
                event.setStatus(OutboxStatus.FAILED);
            }
        }
    }

    private void updateFinalStatuses(List<OutboxEvent> events) {
        outboxEventRepository.saveAll(events);
    }
}
