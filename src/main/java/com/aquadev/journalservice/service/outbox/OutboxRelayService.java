package com.aquadev.journalservice.service.outbox;

import com.aquadev.journalservice.model.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OutboxRelayService {

    private final OutboxRelayDao outboxRelayDao;
    private final KafkaTemplate<String, String> outboxKafkaTemplate;

    public OutboxRelayService(OutboxRelayDao outboxRelayDao, @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> outboxKafkaTemplate) {
        this.outboxRelayDao = outboxRelayDao;
        this.outboxKafkaTemplate = outboxKafkaTemplate;
    }

    @Scheduled(fixedRateString = "${outbox.relay-interval-ms:5000}")
    public void relay() {
        List<OutboxEvent> events = outboxRelayDao.lockAndMarkProcessing();
        if (events.isEmpty()) return;

        log.debug("Processing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            boolean sent = sendToKafka(event);
            outboxRelayDao.updateStatus(event, sent);
        }
    }

    private boolean sendToKafka(OutboxEvent event) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    event.getTopic(), null, event.getKey(), event.getPayload());

            record.headers()
                    .add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8))
                    .add("aggregateType", event.getAggregateType().getBytes(StandardCharsets.UTF_8));

            outboxKafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
            log.debug("Sent outbox event {} to topic={}", event.getId(), event.getTopic());
            return true;
        } catch (Exception e) {
            log.warn("Failed to send outbox event {}: {}", event.getId(), e.getMessage());
            return false;
        }
    }
}
