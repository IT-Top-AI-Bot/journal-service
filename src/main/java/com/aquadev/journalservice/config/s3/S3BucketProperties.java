package com.aquadev.journalservice.config.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3")
public record S3BucketProperties(
        String bucket
) {
}
