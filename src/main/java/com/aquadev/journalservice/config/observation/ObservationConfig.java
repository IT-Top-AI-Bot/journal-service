package com.aquadev.journalservice.config.observation;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
class ObservationConfig {

    @Bean
    ObservationRegistryCustomizer<ObservationRegistry> skipActuatorObservations() {
        return registry -> registry.observationConfig()
                .observationPredicate((_, context) -> {
                    if (context instanceof ServerRequestObservationContext serverContext) {
                        var carrier = serverContext.getCarrier();
                        return carrier == null || !carrier.getRequestURI().startsWith("/actuator");
                    }
                    return true;
                });
    }
}
