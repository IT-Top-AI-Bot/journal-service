package com.aquadev.ittopai.service.outbox;

import com.aquadev.ittopai.model.OutboxEvent;
import com.aquadev.ittopai.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;

    @Resource(name = "outboxObjectMapper")
    private ObjectMapper objectMapper;

    public void publish(String aggregateType,
                        String aggregateId,
                        String eventType,
                        String topic,
                        Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize outbox payload for aggregateId=" + aggregateId, e);
        }
        OutboxEvent event = OutboxEvent.newEvent(
                aggregateType, aggregateId, eventType, topic, UUID.randomUUID().toString(), json);
        outboxEventRepository.save(event);
    }
}