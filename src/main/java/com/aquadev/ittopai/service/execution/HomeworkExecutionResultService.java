package com.aquadev.ittopai.service.execution;

import com.aquadev.ittopai.dto.kafka.HomeworkExecutionResultEvent;

public interface HomeworkExecutionResultService {

    void handleEvent(HomeworkExecutionResultEvent event);
}
