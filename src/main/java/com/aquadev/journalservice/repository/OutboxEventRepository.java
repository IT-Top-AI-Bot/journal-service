package com.aquadev.journalservice.repository;

import com.aquadev.journalservice.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
            select *
            from outbox_event
            where status = 'NEW'
              and next_attempt_at <= :now
            order by created_at
            for update skip locked
            limit :batchSize
            """, nativeQuery = true)
    List<OutboxEvent> lockBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
