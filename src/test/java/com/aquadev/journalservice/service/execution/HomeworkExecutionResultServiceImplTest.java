package com.aquadev.journalservice.service.execution;

import com.aquadev.commonlibs.HomeworkExecutionResultEvent;
import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.config.s3.S3BucketProperties;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.service.execution.HomeworkExecutionPersistenceService.ExecutionWithTelegramId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeworkExecutionResultServiceImplTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private JournalClient journalClient;
    @Mock
    private S3BucketProperties s3BucketProperties;
    @Mock
    private HomeworkExecutionPersistenceService persistenceService;

    @InjectMocks
    private HomeworkExecutionResultServiceImpl resultService;

    @Test
    void handleEvent_success_processesUpload() {
        UUID executionId = UUID.randomUUID();
        HomeworkExecutionResultEvent event = new HomeworkExecutionResultEvent(
                executionId,
                HomeworkExecutionStatus.DONE,
                "s3-key",
                null,
                null,
                Instant.now()
        );

        HomeworkExecution execution = new HomeworkExecution();
        execution.setId(executionId);
        execution.setHomeworkId(42L);
        execution.setResultS3Key("s3-key");

        ExecutionWithTelegramId result = new ExecutionWithTelegramId(execution, 12345L);

        when(persistenceService.updateExecutionBaseInfo(event)).thenReturn(result);
        when(s3BucketProperties.bucket()).thenReturn("test-bucket");

        byte[] content = "test content".getBytes();
        ResponseBytes<GetObjectResponse> responseBytes = mock(ResponseBytes.class);
        when(responseBytes.asByteArray()).thenReturn(content);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        resultService.handleEvent(event);

        verify(journalClient).uploadHomework(eq(42L), any(InputStream.class), eq((long) content.length));
    }

    @Test
    void handleEvent_failed_skipsJournalUpload() {
        UUID executionId = UUID.randomUUID();
        HomeworkExecutionResultEvent event = new HomeworkExecutionResultEvent(
                executionId,
                HomeworkExecutionStatus.FAILED,
                null,
                null,
                "AI solver error",
                Instant.now()
        );

        HomeworkExecution execution = new HomeworkExecution();
        execution.setId(executionId);

        when(persistenceService.updateExecutionBaseInfo(event))
                .thenReturn(new ExecutionWithTelegramId(execution, 12345L));

        resultService.handleEvent(event);

        verify(journalClient, never()).uploadHomework(any(), any(), anyLong());
        verify(journalClient, never()).uploadHomeworkText(any(), any());
        verify(persistenceService, never()).updateStatusToFailed(any());
    }

    @Test
    void handleEvent_s3Fails_setsStatusToFailed() {
        UUID executionId = UUID.randomUUID();
        HomeworkExecutionResultEvent event = new HomeworkExecutionResultEvent(
                executionId,
                HomeworkExecutionStatus.DONE,
                "s3-key",
                null,
                null,
                Instant.now()
        );

        HomeworkExecution execution = new HomeworkExecution();
        execution.setId(executionId);
        execution.setResultS3Key("s3-key");

        ExecutionWithTelegramId result = new ExecutionWithTelegramId(execution, 12345L);

        when(persistenceService.updateExecutionBaseInfo(event)).thenReturn(result);
        when(s3BucketProperties.bucket()).thenReturn("test-bucket");
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(new RuntimeException("S3 error"));

        resultService.handleEvent(event);

        verify(persistenceService).updateStatusToFailed(executionId);
    }
}
