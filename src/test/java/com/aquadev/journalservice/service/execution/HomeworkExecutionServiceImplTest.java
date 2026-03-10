package com.aquadev.journalservice.service.execution;

import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.repository.HomeworkExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeworkExecutionServiceImplTest {

    @Mock HomeworkExecutionRepository homeworkExecutionRepository;

    @InjectMocks
    HomeworkExecutionServiceImpl service;

    @Test
    void complete_found_setsStatusDoneAndS3Key() {
        UUID id = UUID.randomUUID();
        HomeworkExecution execution = new HomeworkExecution();
        execution.setStatus(HomeworkExecutionStatus.PENDING);

        when(homeworkExecutionRepository.findById(id)).thenReturn(Optional.of(execution));

        service.complete(id, "bucket/result.docx");

        assertThat(execution.getStatus()).isEqualTo(HomeworkExecutionStatus.DONE);
        assertThat(execution.getResultS3Key()).isEqualTo("bucket/result.docx");
        assertThat(execution.getCompletedAt()).isNotNull();
        verify(homeworkExecutionRepository).save(execution);
    }

    @Test
    void complete_notFound_throwsIllegalArgument() {
        // NOTE: throws IllegalArgumentException, not HomeworkExecutionNotFoundException —
        // inconsistency with HomeworkExecutionPersistenceService which uses the domain exception.
        UUID id = UUID.randomUUID();
        when(homeworkExecutionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(id, "some-key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void complete_setsCompletedAtTimestamp() {
        UUID id = UUID.randomUUID();
        HomeworkExecution execution = new HomeworkExecution();
        when(homeworkExecutionRepository.findById(id)).thenReturn(Optional.of(execution));

        service.complete(id, "key");

        assertThat(execution.getCompletedAt()).isNotNull();
    }

    @Test
    void complete_savesWithCaptor() {
        UUID id = UUID.randomUUID();
        HomeworkExecution execution = new HomeworkExecution();
        when(homeworkExecutionRepository.findById(id)).thenReturn(Optional.of(execution));

        service.complete(id, "final-key");

        ArgumentCaptor<HomeworkExecution> captor = ArgumentCaptor.forClass(HomeworkExecution.class);
        verify(homeworkExecutionRepository).save(captor.capture());
        assertThat(captor.getValue().getResultS3Key()).isEqualTo("final-key");
        assertThat(captor.getValue().getStatus()).isEqualTo(HomeworkExecutionStatus.DONE);
    }
}
