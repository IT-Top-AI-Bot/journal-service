package com.aquadev.journalservice.repository;

import com.aquadev.journalservice.model.JournalCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JournalCredentialRepository extends JpaRepository<JournalCredential, Long> {

    Optional<JournalCredential> findByJournalUserId(Long journalUserId);
}
