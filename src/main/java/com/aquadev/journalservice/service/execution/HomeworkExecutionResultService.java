package com.aquadev.journalservice.service.execution;

import com.aquadev.journalservice.dto.kafka.HomeworkExecutionResultEvent;

public interface HomeworkExecutionResultService {

    void handleEvent(HomeworkExecutionResultEvent event);
}
