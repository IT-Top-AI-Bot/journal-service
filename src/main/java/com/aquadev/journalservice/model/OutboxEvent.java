package com.aquadev.journalservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "key", nullable = false, length = 200)
    private String key;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public static OutboxEvent newEvent(
            String aggregateType,
            String aggregateId,
            String eventType,
            String topic,
            String key,
            String payloadJson
    ) {
        Instant now = Instant.now();
        return OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .topic(topic)
                .key(key)
                .payload(payloadJson)
                .status(OutboxStatus.NEW)
                .attempts(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .build();
    }

    public void markProcessing(String locker) {
        this.status = OutboxStatus.PROCESSING;
        this.lockedBy = locker;
        this.lockedAt = Instant.now();
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = Instant.now();
        this.lockedBy = null;
        this.lockedAt = null;
    }

    public void markRetry(Instant nextAttemptAt) {
        this.status = OutboxStatus.NEW;
        this.attempts++;
        this.nextAttemptAt = nextAttemptAt;
        this.lockedBy = null;
        this.lockedAt = null;
    }

    public void markErrorFinal() {
        this.status = OutboxStatus.ERROR;
        this.attempts++;
        this.lockedBy = null;
        this.lockedAt = null;
    }
}
