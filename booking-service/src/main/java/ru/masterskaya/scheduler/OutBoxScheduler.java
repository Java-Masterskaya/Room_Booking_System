package ru.masterskaya.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.masterskaya.service.OutboxProcessor;

@Component
@ConditionalOnProperty(name = "app.outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutBoxScheduler {

    private final OutboxProcessor outboxProcessor;

    @Scheduled(fixedDelay = 1000)
    public void processOutboxEvent() {
        outboxProcessor.sendOutboxToKafka();
    }
}
