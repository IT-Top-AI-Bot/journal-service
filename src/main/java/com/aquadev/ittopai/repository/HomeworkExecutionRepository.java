package com.aquadev.ittopai.repository;

import com.aquadev.ittopai.model.HomeworkExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HomeworkExecutionRepository extends JpaRepository<HomeworkExecution, UUID> {
}