package com.aquadev.journalservice.consumer;

import com.aquadev.commonlibs.HomeworkExecutionResultEvent;
import com.aquadev.journalservice.service.execution.HomeworkExecutionResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomeworkResultConsumer {

    private final HomeworkExecutionResultService homeworkExecutionResultService;

    @KafkaListener(topics = "${kafka.homework-execution-result}", containerFactory = "homeworkResultListenerContainerFactory")
    public void consume(HomeworkExecutionResultEvent event) {
        log.info("Received homework execution result event: {}", event);
        homeworkExecutionResultService.handleEvent(event);
    }
}
