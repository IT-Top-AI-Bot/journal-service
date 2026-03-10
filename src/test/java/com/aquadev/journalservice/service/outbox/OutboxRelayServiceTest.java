package com.aquadev.journalservice.service.outbox;

import com.aquadev.journalservice.model.OutboxEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    OutboxRelayDao outboxRelayDao;

    @Mock
    KafkaTemplate<String, String> outboxKafkaTemplate;

    OutboxRelayService relayService;

    @BeforeEach
    void setUp() {
        relayService = new OutboxRelayService(outboxRelayDao, outboxKafkaTemplate);
    }

    @Captor
    private ArgumentCaptor<ProducerRecord<String, String>> captor;

    // ── relay: no events ──────────────────────────────────────────────────────

    @Test
    void relay_noEvents_doesNothing() {
        setUp();
        when(outboxRelayDao.lockAndMarkProcessing()).thenReturn(List.of());

        relayService.relay();

        verifyNoInteractions(outboxKafkaTemplate);
        verify(outboxRelayDao, never()).updateStatus(any(), anyBoolean());
    }

    // ── relay: send success ───────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void relay_oneEvent_sendsToKafkaAndMarksSent() {
        setUp();
        OutboxEvent event = makeEvent("my-topic", "my-key", "{\"x\":1}", "HomeworkExecution", "Created");

        when(outboxRelayDao.lockAndMarkProcessing()).thenReturn(List.of(event));
        when(outboxKafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        relayService.relay();

        verify(outboxRelayDao).updateStatus(event, true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void relay_kafkaSendFails_marksNotSent() {
        setUp();
        OutboxEvent event = makeEvent("topic", "key", "{}", "AggType", "EvtType");

        when(outboxRelayDao.lockAndMarkProcessing()).thenReturn(List.of(event));
        when(outboxKafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

        relayService.relay();

        verify(outboxRelayDao).updateStatus(event, false);
    }

    // ── relay: Kafka record headers ───────────────────────────────────────────

    @Test
    void relay_producerRecordHasCorrectTopicAndPayload() {
        setUp();
        OutboxEvent event = makeEvent("exec-topic", "exec-key", "{\"id\":\"123\"}", "HomeworkExecution", "Created");

        when(outboxRelayDao.lockAndMarkProcessing()).thenReturn(List.of(event));
        when(outboxKafkaTemplate.send(ArgumentMatchers.<ProducerRecord<String, String>>any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        relayService.relay();

        verify(outboxKafkaTemplate).send(captor.capture());

        ProducerRecord<String, String> producerRecord = captor.getValue();
        assertThat(producerRecord.topic()).isEqualTo("exec-topic");
        assertThat(producerRecord.key()).isEqualTo("exec-key");
        assertThat(producerRecord.value()).isEqualTo("{\"id\":\"123\"}");
        assertThat(producerRecord.headers().lastHeader("eventType")).isNotNull();
        assertThat(new String(producerRecord.headers().lastHeader("eventType").value())).isEqualTo("Created");
        assertThat(new String(producerRecord.headers().lastHeader("aggregateType").value())).isEqualTo("HomeworkExecution");
    }

    // ── relay: multiple events ────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void relay_multipleEvents_processesAll() {
        setUp();
        OutboxEvent e1 = makeEvent("t", "k1", "{}", "T", "E");
        OutboxEvent e2 = makeEvent("t", "k2", "{}", "T", "E");
        OutboxEvent e3 = makeEvent("t", "k3", "{}", "T", "E");

        when(outboxRelayDao.lockAndMarkProcessing()).thenReturn(List.of(e1, e2, e3));
        when(outboxKafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        relayService.relay();

        verify(outboxRelayDao, times(3)).updateStatus(any(), eq(true));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private OutboxEvent makeEvent(String topic, String key, String payload,
                                  String aggregateType, String eventType) {
        OutboxEvent e = OutboxEvent.newEvent(aggregateType, "agg-id", eventType, topic, key, payload);
        ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
        return e;
    }
}
