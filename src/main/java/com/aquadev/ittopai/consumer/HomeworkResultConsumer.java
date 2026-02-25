package com.aquadev.ittopai.consumer;

import com.aquadev.ittopai.dto.kafka.HomeworkExecutionResultEvent;
import com.aquadev.ittopai.service.execution.HomeworkExecutionResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomeworkResultConsumer {

    private final HomeworkExecutionResultService homeworkExecutionResultService;

    //    @KafkaListener(topics = "${kafka.homework-execution-result}")
    public void consume(HomeworkExecutionResultEvent event) {
        log.info("Received homework execution result event: {}", event);
        homeworkExecutionResultService.handleEvent(event);
    }
}
