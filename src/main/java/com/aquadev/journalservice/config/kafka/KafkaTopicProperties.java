package com.aquadev.journalservice.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
public record KafkaTopicProperties(
        String homeworkExecutionTopic
) {
}
