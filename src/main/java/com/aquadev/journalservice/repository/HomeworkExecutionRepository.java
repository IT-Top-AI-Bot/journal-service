package com.aquadev.journalservice.repository;

import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HomeworkExecutionRepository extends JpaRepository<HomeworkExecution, UUID> {

    boolean existsByUserAndHomeworkId(User user, Long homeworkId);

}