package com.aquadev.journalservice.service.outbox;

import com.aquadev.journalservice.config.outbox.OutboxProperties;
import com.aquadev.journalservice.model.OutboxEvent;
import com.aquadev.journalservice.model.OutboxStatus;
import com.aquadev.journalservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayDaoTest {

    @Mock OutboxProperties outboxProperties;
    @Mock OutboxEventRepository outboxEventRepository;

    @InjectMocks
    OutboxRelayDao outboxRelayDao;

    // ── lockAndMarkProcessing ─────────────────────────────────────────────────

    @Test
    void lockAndMarkProcessing_returnsEventsMarkedProcessing() {
        when(outboxProperties.batchSize()).thenReturn(10);
        OutboxEvent e1 = OutboxEvent.newEvent("T", "1", "E", "topic", "k1", "{}");
        OutboxEvent e2 = OutboxEvent.newEvent("T", "2", "E", "topic", "k2", "{}");
        when(outboxEventRepository.lockBatch(any(Instant.class), eq(10)))
                .thenReturn(List.of(e1, e2));

        List<OutboxEvent> result = outboxRelayDao.lockAndMarkProcessing();

        assertThat(result).hasSize(2);
        assertThat(e1.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(e2.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(e1.getLockedBy()).isNotNull();
        assertThat(e2.getLockedBy()).isNotNull();
    }

    @Test
    void lockAndMarkProcessing_allEventsSameLockerId() {
        when(outboxProperties.batchSize()).thenReturn(5);
        OutboxEvent e1 = OutboxEvent.newEvent("T", "1", "E", "t", "k", "{}");
        OutboxEvent e2 = OutboxEvent.newEvent("T", "2", "E", "t", "k", "{}");
        when(outboxEventRepository.lockBatch(any(), anyInt())).thenReturn(List.of(e1, e2));

        outboxRelayDao.lockAndMarkProcessing();

        // Both events locked by same lockerId in this batch
        assertThat(e1.getLockedBy()).isEqualTo(e2.getLockedBy());
    }

    // ── updateStatus: sent ────────────────────────────────────────────────────

    @Test
    void updateStatus_sent_marksEventSent() {
        OutboxEvent event = OutboxEvent.newEvent("T", "1", "E", "topic", "k", "{}");
        UUID id = UUID.randomUUID();
        // Use reflection to set id since it's normally set by JPA
        setId(event, id);

        OutboxEvent managed = OutboxEvent.newEvent("T", "1", "E", "topic", "k", "{}");
        setId(managed, id);
        managed.markProcessing("worker");

        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(managed));

        outboxRelayDao.updateStatus(event, true);

        assertThat(managed.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(managed.getSentAt()).isNotNull();
    }

    // ── updateStatus: retry ───────────────────────────────────────────────────

    @Test
    void updateStatus_failedBelowMaxAttempts_marksRetry() {
        when(outboxProperties.maxAttempts()).thenReturn(3);
        when(outboxProperties.retryDelaySeconds()).thenReturn(30L);

        UUID id = UUID.randomUUID();
        OutboxEvent stub = OutboxEvent.newEvent("T", "1", "E", "t", "k", "{}");
        setId(stub, id);

        OutboxEvent managed = OutboxEvent.newEvent("T", "1", "E", "t", "k", "{}");
        setId(managed, id);
        managed.markProcessing("w");
        // attempts = 0 → 0 < 3 → should retry

        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(managed));

        outboxRelayDao.updateStatus(stub, false);

        assertThat(managed.getStatus()).isEqualTo(OutboxStatus.NEW);
        assertThat(managed.getAttempts()).isEqualTo(1);
    }

    // ── updateStatus: error final ─────────────────────────────────────────────

    @Test
    void updateStatus_failedAtMaxAttempts_marksErrorFinal() {
        when(outboxProperties.maxAttempts()).thenReturn(3);

        UUID id = UUID.randomUUID();
        OutboxEvent stub = OutboxEvent.newEvent("T", "1", "E", "t", "k", "{}");
        setId(stub, id);

        OutboxEvent managed = OutboxEvent.newEvent("T", "1", "E", "t", "k", "{}");
        setId(managed, id);
        // Simulate 3 prior retries → attempts = 3
        managed.markRetry(Instant.now()); // attempts = 1
        managed.markRetry(Instant.now()); // attempts = 2
        managed.markRetry(Instant.now()); // attempts = 3
        managed.markProcessing("w");

        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(managed));

        outboxRelayDao.updateStatus(stub, false);

        assertThat(managed.getStatus()).isEqualTo(OutboxStatus.ERROR);
        // NOTE: markErrorFinal() increments attempts, so it becomes 4
        assertThat(managed.getAttempts()).isEqualTo(4);
    }

    @Test
    void updateStatus_eventNotFound_throwsIllegalState() {
        UUID id = UUID.randomUUID();
        OutboxEvent stub = OutboxEvent.newEvent("T", "1", "E", "t", "k", "{}");
        setId(stub, id);

        when(outboxEventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> outboxRelayDao.updateStatus(stub, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Event not found");
    }

    private void setId(OutboxEvent event, UUID id) {
        org.springframework.test.util.ReflectionTestUtils.setField(event, "id", id);
    }
}
