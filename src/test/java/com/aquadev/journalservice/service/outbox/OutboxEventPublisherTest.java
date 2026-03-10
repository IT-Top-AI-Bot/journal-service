package com.aquadev.journalservice.service.outbox;

import com.aquadev.journalservice.model.OutboxEvent;
import com.aquadev.journalservice.model.OutboxStatus;
import com.aquadev.journalservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock OutboxEventRepository outboxEventRepository;

    @InjectMocks
    OutboxEventPublisher publisher;

    @BeforeEach
    void injectObjectMapper() {
        // Inject real ObjectMapper since it's @Resource (not constructor injection)
        ReflectionTestUtils.setField(publisher, "objectMapper", new ObjectMapper());
    }

    @Test
    void publish_savesOutboxEventWithCorrectFields() {
        record Payload(String field) {}
        Payload payload = new Payload("value");

        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        publisher.publish("HomeworkExecution", "agg-1", "Created", "topic-name", payload);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("HomeworkExecution");
        assertThat(saved.getAggregateId()).isEqualTo("agg-1");
        assertThat(saved.getEventType()).isEqualTo("Created");
        assertThat(saved.getTopic()).isEqualTo("topic-name");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.NEW);
        assertThat(saved.getPayload()).contains("value");
        assertThat(saved.getId()).isNull(); // must be null for JPA @GeneratedValue
    }

    @Test
    void publish_nonSerializablePayload_throwsIllegalState() {
        // ObjectMapper cannot serialize objects with circular references
        Object circular = new Object() {
            // Override toString to prevent default serialization issues
            @Override
            public String toString() { return "circular"; }
        };

        // Replace objectMapper with a broken one that always fails
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        try {
            when(brokenMapper.writeValueAsString(any()))
                    .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("fail") {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(publisher, "objectMapper", brokenMapper);

        assertThatThrownBy(() -> publisher.publish("T", "id", "evt", "topic", circular))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize outbox payload");

        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void publish_keyIsRandomUuidEachTime() {
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        publisher.publish("T", "1", "E", "topic", "payload1");
        publisher.publish("T", "2", "E", "topic", "payload2");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(captor.capture());

        String key1 = captor.getAllValues().get(0).getKey();
        String key2 = captor.getAllValues().get(1).getKey();
        assertThat(key1).isNotEqualTo(key2);
    }
}
