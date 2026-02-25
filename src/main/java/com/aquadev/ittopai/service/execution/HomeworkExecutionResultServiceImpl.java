package com.aquadev.ittopai.service.execution;

import com.aquadev.ittopai.dto.kafka.HomeworkExecutionResultEvent;
import org.springframework.stereotype.Service;

@Service
public class HomeworkExecutionResultServiceImpl implements HomeworkExecutionResultService {
    @Override
    public void handleEvent(HomeworkExecutionResultEvent event) {

    }
}
