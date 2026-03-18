package com.aquadev.journalservice.service.execution;

import com.aquadev.commonlibs.HomeworkExecutionResultEvent;
import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.exception.domain.homeworkexecution.HomeworkExecutionNotFoundException;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.repository.HomeworkExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeworkExecutionPersistenceServiceTest {

    @Mock
    private HomeworkExecutionRepository homeworkExecutionRepository;

    @InjectMocks
    private HomeworkExecutionPersistenceService persistenceService;

    @Test
    void updateExecutionBaseInfo_success_updatesAndReturns() {
        UUID executionId = UUID.randomUUID();
        Instant now = Instant.now();
        HomeworkExecutionResultEvent event = new HomeworkExecutionResultEvent(
                executionId,
                HomeworkExecutionStatus.DONE,
                "s3-key",
                null,
                null,
                now
        );

        User user = new User();
        user.setTelegramId(12345L);
        HomeworkExecution execution = new HomeworkExecution();
        execution.setId(executionId);
        execution.setUser(user);

        when(homeworkExecutionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        when(homeworkExecutionRepository.save(any(HomeworkExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HomeworkExecutionPersistenceService.ExecutionWithTelegramId result = persistenceService.updateExecutionBaseInfo(event);

        assertThat(result.execution().getStatus()).isEqualTo(HomeworkExecutionStatus.DONE);
        assertThat(result.execution().getResultS3Key()).isEqualTo("s3-key");
        assertThat(result.execution().getCompletedAt()).isEqualTo(now);
        assertThat(result.telegramId()).isEqualTo(12345L);

        verify(homeworkExecutionRepository).save(execution);
    }

    @Test
    void updateExecutionBaseInfo_notFound_throwsException() {
        UUID executionId = UUID.randomUUID();
        HomeworkExecutionResultEvent event = new HomeworkExecutionResultEvent(
                executionId,
                HomeworkExecutionStatus.DONE,
                "s3-key",
                null,
                null,
                Instant.now()
        );

        when(homeworkExecutionRepository.findById(executionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> persistenceService.updateExecutionBaseInfo(event))
                .isInstanceOf(HomeworkExecutionNotFoundException.class);
    }

    @Test
    void updateStatusToFailed_success_updatesStatus() {
        UUID executionId = UUID.randomUUID();
        HomeworkExecution execution = new HomeworkExecution();
        execution.setId(executionId);
        execution.setStatus(HomeworkExecutionStatus.PENDING);

        when(homeworkExecutionRepository.findById(executionId)).thenReturn(Optional.of(execution));

        persistenceService.updateStatusToFailed(executionId);

        assertThat(execution.getStatus()).isEqualTo(HomeworkExecutionStatus.FAILED);
        verify(homeworkExecutionRepository).save(execution);
    }

    @Test
    void updateStatusToFailed_notFound_doesNothing() {
        UUID executionId = UUID.randomUUID();
        when(homeworkExecutionRepository.findById(executionId)).thenReturn(Optional.empty());

        persistenceService.updateStatusToFailed(executionId);

        verify(homeworkExecutionRepository, never()).save(any());
    }
}
