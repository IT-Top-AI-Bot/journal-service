package com.aquadev.journalservice.repository;

import com.aquadev.journalservice.model.JournalToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalTokenRepository extends JpaRepository<JournalToken, Long> {
}
