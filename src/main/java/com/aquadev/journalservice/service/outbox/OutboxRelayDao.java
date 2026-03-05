package com.aquadev.journalservice.service.outbox;

import com.aquadev.journalservice.config.outbox.OutboxProperties;
import com.aquadev.journalservice.model.OutboxEvent;
import com.aquadev.journalservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayDao {

    private final OutboxProperties outboxProperties;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public List<OutboxEvent> lockAndMarkProcessing() {
        List<OutboxEvent> events = outboxEventRepository.lockBatch(
                Instant.now(), outboxProperties.batchSize());
        String lockerId = UUID.randomUUID().toString();
        events.forEach(e -> e.markProcessing(lockerId));
        return events;
    }

    @Transactional
    public void updateStatus(OutboxEvent event, boolean sent) {
        OutboxEvent managed = outboxEventRepository.findById(event.getId())
                .orElseThrow(() -> new IllegalStateException("Event not found: " + event.getId()));
        if (sent) {
            managed.markSent();
        } else if (managed.getAttempts() >= outboxProperties.maxAttempts()) {
            log.error("Outbox event {} exceeded max attempts", managed.getId());
            managed.markErrorFinal();
        } else {
            managed.markRetry(Instant.now().plusSeconds(outboxProperties.retryDelaySeconds()));
        }
    }
}
