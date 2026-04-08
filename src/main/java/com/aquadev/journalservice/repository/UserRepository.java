package com.aquadev.journalservice.repository;

import com.aquadev.journalservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTelegramId(Long telegramId);

    @Query("select u from User u join fetch u.journalUser ju join fetch ju.journalGroups where u.telegramId = :telegramId")
    Optional<User> findByTelegramIdWithGroups(@Param("telegramId") Long telegramId);

    @Query("select u.journalCredential.journalUserId from User u where u.telegramId = :telegramId")
    Optional<Long> findJournalUserIdByTelegramId(@Param("telegramId") Long telegramId);

    boolean existsByTelegramId(Long telegramId);
}
