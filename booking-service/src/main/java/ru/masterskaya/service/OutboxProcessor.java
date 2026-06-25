package ru.masterskaya.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.masterskaya.model.OutboxEvent;
import ru.masterskaya.model.OutboxStatus;
import ru.masterskaya.repository.OutboxEventRepository;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "booking.reservation.v1";

    @Transactional
    public void sendOutboxToKafka() {
        List<OutboxEvent> events = outboxEventRepository.findNewEvents(PageRequest.of(0, 50));

        if (events == null || events.isEmpty()) {
            return;
        }

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayLoad())
                        .get(5, TimeUnit.SECONDS);
                event.setStatus(OutboxStatus.PROCESSED);
            } catch (Exception exception) {
                log.error("Критический сбой при обработке события {}: {}", event.getId(), exception.getMessage());
                event.setStatus(OutboxStatus.FAILED);
            }
        }
    }
}
