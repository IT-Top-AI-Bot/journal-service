package com.aquadev.journalservice.service.execution;

import com.aquadev.commonlibs.HomeworkExecutionResultEvent;
import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.exception.domain.homeworkexecution.HomeworkExecutionNotFoundException;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.repository.HomeworkExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class HomeworkExecutionPersistenceService {

    private final HomeworkExecutionRepository homeworkExecutionRepository;

    record ExecutionWithTelegramId(HomeworkExecution execution, Long telegramId) {}

    @Transactional
    public ExecutionWithTelegramId updateExecutionBaseInfo(@NonNull HomeworkExecutionResultEvent event) {
        HomeworkExecution execution = homeworkExecutionRepository.findById(event.executionId())
                .orElseThrow(HomeworkExecutionNotFoundException::new);

        execution.setStatus(event.status());
        execution.setResultS3Key(event.resultS3Key());
        execution.setCompletedAt(event.createdAt());

        HomeworkExecution saved = homeworkExecutionRepository.save(execution);
        Long telegramId = saved.getUser().getTelegramId();

        log.info("HomeworkExecution {} basic info updated", event.executionId());
        return new ExecutionWithTelegramId(saved, telegramId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusToFailed(UUID executionId) {
        homeworkExecutionRepository.findById(executionId).ifPresent(ex -> {
            ex.setStatus(HomeworkExecutionStatus.FAILED);
            homeworkExecutionRepository.save(ex);
        });
    }
}
