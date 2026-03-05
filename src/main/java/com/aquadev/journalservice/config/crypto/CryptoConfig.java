package com.aquadev.journalservice.config.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
public class CryptoConfig {

    @Value("${crypto.secret-key}")
    private String secretKey;

    @Bean
    public SecretKey aesKey() {
        byte[] keyBytes = Arrays.copyOf(secretKey.getBytes(StandardCharsets.UTF_8), 16); // AES-128
        return new SecretKeySpec(keyBytes, "AES");
    }
}
