package com.aquadev.journalservice.repository;

import com.aquadev.journalservice.model.UserAutoHomeworkSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAutoHomeworkSettingsRepository extends JpaRepository<UserAutoHomeworkSettings, UUID> {

    Optional<UserAutoHomeworkSettings> findByUserId(UUID userId);

    @Query("SELECT DISTINCT s FROM UserAutoHomeworkSettings s " +
           "JOIN FETCH s.user u " +
           "JOIN FETCH u.journalUser ju " +
           "LEFT JOIN FETCH ju.journalGroups " +
           "LEFT JOIN FETCH s.specIds " +
           "WHERE s.enabled = true")
    List<UserAutoHomeworkSettings> findAllEnabledWithUserData();
}
