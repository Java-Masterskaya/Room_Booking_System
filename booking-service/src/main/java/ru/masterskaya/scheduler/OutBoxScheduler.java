package ru.masterskaya.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    private static final String TOPIC = "booking.reservation.v1";

    @Transactional
    @Scheduled(fixedDelay = 1000)
    public void processOutboxEvent() {
        List<OutboxEvent> events = outboxEventRepository.findNewEvents(PageRequest.of(0, 50));

        if (events.isEmpty()) {
            return;
        }

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

        outboxEventRepository.saveAll(events);
    }
}
