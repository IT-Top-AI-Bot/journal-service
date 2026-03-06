package com.aquadev.journalservice.service.execution;

import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.repository.HomeworkExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkExecutionServiceImpl implements HomeworkExecutionService {

    private final HomeworkExecutionRepository homeworkExecutionRepository;

    @Override
    @Transactional
    public void complete(UUID id, String s3Key) {
        HomeworkExecution execution = homeworkExecutionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("HomeworkExecution not found: " + id));

        execution.setStatus(HomeworkExecutionStatus.DONE);
        execution.setResultS3Key(s3Key);
        execution.setCompletedAt(Instant.now());

        homeworkExecutionRepository.save(execution);
        log.info("HomeworkExecution {} completed, s3Key={}", id, s3Key);
    }
}
