package com.aquadev.journalservice.service.execution;

import com.aquadev.commonlibs.HomeworkExecutionEvent;
import com.aquadev.journalservice.config.kafka.KafkaTopicProperties;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.service.outbox.OutboxEventPublisher;
import com.aquadev.journalservice.tracing.HomeworkExecutionSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeworkExecutionEventPublisher {

    private static final String AGGREGATE_TYPE = "HomeworkExecution";
    private static final String EVENT_TYPE = "HomeworkExecutionCreated";

    private final OutboxEventPublisher outboxEventPublisher;
    private final KafkaTopicProperties kafkaProperties;
    private final HomeworkExecutionSpan homeworkExecutionSpan;

    public void publishCreated(HomeworkExecution execution) {
        Long telegramUserId = execution.getUser() != null ? execution.getUser().getTelegramId() : null;

        homeworkExecutionSpan.run(execution, () ->
                outboxEventPublisher.publish(
                        AGGREGATE_TYPE,
                        execution.getId().toString(),
                        EVENT_TYPE,
                        kafkaProperties.homeworkExecutionTopic(),
                        new HomeworkExecutionEvent(
                                execution.getId(),
                                execution.getTheme(),
                                execution.getSpecId(),
                                execution.getStatus(),
                                execution.getComment(),
                                execution.getGroupId(),
                                execution.getTeachId(),
                                execution.getNameSpec(),
                                execution.getCreatedAt(),
                                execution.getHomeworkId(),
                                execution.getTeacherFio(),
                                execution.getHomeworkUrl(),
                                execution.getOverdueTime(),
                                execution.getCompletionTime(),
                                telegramUserId
                        )
                )
        );
    }
}
