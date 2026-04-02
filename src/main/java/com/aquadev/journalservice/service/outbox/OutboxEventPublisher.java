package com.aquadev.journalservice.service.outbox;

import com.aquadev.journalservice.model.OutboxEvent;
import com.aquadev.journalservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
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
                aggregateType, aggregateId, eventType, topic, UUID.randomUUID().toString(), json, currentTraceparent());
        outboxEventRepository.save(event);
    }

    private static String currentTraceparent() {
        SpanContext ctx = Span.current().getSpanContext();
        if (!ctx.isValid()) {
            return null;
        }
        return "00-" + ctx.getTraceId() + "-" + ctx.getSpanId() + "-" + ctx.getTraceFlags().asHex();
    }
}