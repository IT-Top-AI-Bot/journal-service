package com.aquadev.journalservice.config.tracing;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public ObservationPredicate noRedisObservations() {
        return (name, _) -> !name.startsWith("spring.data.redis");
    }
}
