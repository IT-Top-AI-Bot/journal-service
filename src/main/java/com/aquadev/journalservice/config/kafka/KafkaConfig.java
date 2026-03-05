package com.aquadev.journalservice.config.kafka;

import com.aquadev.journalservice.config.outbox.OutboxProperties;
import com.aquadev.journalservice.dto.kafka.HomeworkExecutionResultEvent;
import com.aquadev.journalservice.exception.base.NotFoundException;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
@EnableConfigurationProperties({KafkaTopicProperties.class, OutboxProperties.class})
public class KafkaConfig {

    @Bean
    @SuppressWarnings("removal") // JsonDeserializer deprecated for removal in Spring Kafka — migrate when replacement is available
    public ConsumerFactory<String, HomeworkExecutionResultEvent> homeworkResultConsumerFactory(KafkaProperties bootKafkaProps) {
        Map<String, Object> configs = bootKafkaProps.buildConsumerProperties();
        var jsonDeserializer = new JsonDeserializer<>(HomeworkExecutionResultEvent.class);
        jsonDeserializer.setUseTypeHeaders(false);
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
        return factory;
    }

    @Bean
    public ProducerFactory<String, String> outboxProducerFactory(KafkaProperties bootKafkaProps) {
        Map<String, Object> configs = bootKafkaProps.buildProducerProperties();
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configs);
    }

    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate(ProducerFactory<String, String> outboxProducerFactory) {
        return new KafkaTemplate<>(outboxProducerFactory);
    }
}
