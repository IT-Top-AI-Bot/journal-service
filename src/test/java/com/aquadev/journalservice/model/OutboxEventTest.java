package com.aquadev.journalservice.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    private OutboxEvent makeNew() {
        return OutboxEvent.newEvent(
                "HomeworkExecution",
                "agg-123",
                "HomeworkExecutionCreated",
                "homework-execution-topic",
                "key-123",
                "{\"id\":\"agg-123\"}"
        );
    }

    // ── newEvent ──────────────────────────────────────────────────────────────

    @Test
    void newEvent_setsStatusNew() {
        assertThat(makeNew().getStatus()).isEqualTo(OutboxStatus.NEW);
    }

    @Test
    void newEvent_attemptsIsZero() {
        assertThat(makeNew().getAttempts()).isZero();
    }

    @Test
    void newEvent_idIsNull_soJpaPersistsCorrectly() {
        assertThat(makeNew().getId()).isNull();
    }

    @Test
    void newEvent_nextAttemptAtIsNow() {
        Instant before = Instant.now();
        OutboxEvent event = makeNew();
        Instant after = Instant.now();
        assertThat(event.getNextAttemptAt()).isBetween(before, after);
    }

    @Test
    void newEvent_hasNoLock() {
        OutboxEvent event = makeNew();
        assertThat(event.getLockedBy()).isNull();
        assertThat(event.getLockedAt()).isNull();
    }

    // ── markProcessing ────────────────────────────────────────────────────────

    @Test
    void markProcessing_setsStatusAndLocker() {
        OutboxEvent event = makeNew();
        Instant before = Instant.now();
        event.markProcessing("worker-1");
        Instant after = Instant.now();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(event.getLockedBy()).isEqualTo("worker-1");
        assertThat(event.getLockedAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    // ── markSent ──────────────────────────────────────────────────────────────

    @Test
    void markSent_setsStatusSentAndClearsLock() {
        OutboxEvent event = makeNew();
        event.markProcessing("worker-1");
        Instant before = Instant.now();
        event.markSent();
        Instant after = Instant.now();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getSentAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
        assertThat(event.getLockedBy()).isNull();
        assertThat(event.getLockedAt()).isNull();
    }

    @Test
    void markSent_doesNotIncrementAttempts() {
        OutboxEvent event = makeNew();
        event.markProcessing("worker-1");
        event.markSent();
        assertThat(event.getAttempts()).isZero();
    }

    // ── markRetry ─────────────────────────────────────────────────────────────

    @Test
    void markRetry_resetsStatusToNewAndIncrementsAttempts() {
        OutboxEvent event = makeNew();
        event.markProcessing("worker-1");
        Instant nextAttempt = Instant.now().plusSeconds(60);
        event.markRetry(nextAttempt);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.NEW);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(nextAttempt);
        assertThat(event.getLockedBy()).isNull();
        assertThat(event.getLockedAt()).isNull();
    }

    @Test
    void markRetry_multipleTimes_incrementsCorrectly() {
        OutboxEvent event = makeNew();
        event.markRetry(Instant.now().plusSeconds(10));
        event.markRetry(Instant.now().plusSeconds(20));
        event.markRetry(Instant.now().plusSeconds(30));
        assertThat(event.getAttempts()).isEqualTo(3);
    }

    // ── markErrorFinal ────────────────────────────────────────────────────────

    @Test
    void markErrorFinal_setsStatusErrorAndIncrementsAttempts() {
        OutboxEvent event = makeNew();
        event.markProcessing("worker-1");
        event.markErrorFinal();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.ERROR);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLockedBy()).isNull();
        assertThat(event.getLockedAt()).isNull();
    }

    // ── state machine coverage ────────────────────────────────────────────────

    @Test
    void fullRetryThenErrorFlow() {
        OutboxEvent event = makeNew();
        for (int i = 0; i < 3; i++) {
            event.markProcessing("worker");
            event.markRetry(Instant.now().plusSeconds(5));
        }
        event.markProcessing("worker");
        event.markErrorFinal();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.ERROR);
        assertThat(event.getAttempts()).isEqualTo(4); // 3 retries + 1 final error
    }
}
