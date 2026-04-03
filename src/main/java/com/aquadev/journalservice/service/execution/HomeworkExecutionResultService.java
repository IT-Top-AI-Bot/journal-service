package com.aquadev.journalservice.service.execution;

import com.aquadev.commonlibs.HomeworkExecutionResultEvent;

import java.util.UUID;

public interface HomeworkExecutionResultService {

    void handleEvent(HomeworkExecutionResultEvent event);

    void updateStatusToFailed(UUID executionId);
}
