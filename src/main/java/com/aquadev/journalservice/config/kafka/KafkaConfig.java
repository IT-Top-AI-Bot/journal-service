package com.aquadev.journalservice.config.kafka;

import com.aquadev.commonlibs.HomeworkExecutionResultEvent;
import com.aquadev.journalservice.exception.base.NotFoundException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, HomeworkExecutionResultEvent> homeworkResultConsumerFactory(KafkaProperties bootKafkaProps) {
        Map<String, Object> configs = bootKafkaProps.buildConsumerProperties();
        // useHeadersIfPresent=false: deserialize by target type, not by Kafka message headers
        var jsonDeserializer = new JacksonJsonDeserializer<>(HomeworkExecutionResultEvent.class, false);
        return new DefaultKafkaConsumerFactory<>(configs,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, HomeworkExecutionResultEvent> homeworkResultListenerContainerFactory(
            ConsumerFactory<String, HomeworkExecutionResultEvent> homeworkResultConsumerFactory) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1000L, 2));
        errorHandler.addNotRetryableExceptions(NotFoundException.class);

        ConcurrentKafkaListenerContainerFactory<String, HomeworkExecutionResultEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(homeworkResultConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setObservationEnabled(true);
        return factory;
    }
}
