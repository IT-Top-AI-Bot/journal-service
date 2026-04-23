package com.aquadev.journalservice.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "kafka")
public record KafkaTopicProperties(
        String homeworkExecutionTopic,
        @DefaultValue("notifications") String notificationsTopic
) {
}
