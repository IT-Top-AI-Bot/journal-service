package com.aquadev.journalservice.service.execution;

import com.aquadev.commonlibs.HomeworkExecutionResultEvent;

public interface HomeworkExecutionResultService {

    void handleEvent(HomeworkExecutionResultEvent event);
}
