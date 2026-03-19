package com.aquadev.journalservice.repository;

import com.aquadev.journalservice.model.JournalGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JournalGroupRepository extends JpaRepository<JournalGroup, Long> {

    Optional<JournalGroup> findByJournalGroupId(Long journalGroupId);
}
