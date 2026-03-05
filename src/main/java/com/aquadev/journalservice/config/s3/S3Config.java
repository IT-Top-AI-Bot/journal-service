package com.aquadev.journalservice.config.s3;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(S3BucketProperties.class)
public class S3Config {
}
