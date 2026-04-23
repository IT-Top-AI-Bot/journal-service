package com.aquadev.journalservice.repository;

import com.aquadev.journalservice.model.JournalUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalUserRepository extends JpaRepository<JournalUser, Long> {
}
