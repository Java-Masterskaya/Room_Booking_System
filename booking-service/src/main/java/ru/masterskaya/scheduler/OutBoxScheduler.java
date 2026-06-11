package ru.masterskaya.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import ru.masterskaya.model.OutboxEvent;
import ru.masterskaya.repository.OutboxEventRepository;

import java.util.List;
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
            event.setStatus("PROCESSING");
        }

        return events;
    }

    private void sendEventsToKafka(List<OutboxEvent> events) {
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayLoad())
                        .get(5, TimeUnit.SECONDS);
                event.setStatus("PROCESSED");
            } catch (Exception exception) {
                log.error("Критический сбой при обработке события {}: {}", event.getId(), exception.getMessage());
                event.setStatus("FAILED");
            }
        }
    }

    private void updateFinalStatuses(List<OutboxEvent> events) {
        outboxEventRepository.saveAll(events);
    }
}
