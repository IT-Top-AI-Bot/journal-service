package com.aquadev.ittopai.config.journal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JournalApiProperties.class,
        JournalTokenProperties.class
})
public class JournalConfig {
}
