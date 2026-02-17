package com.aquadev.ittopai.config.client;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!aot")
@EnableFeignClients
public class FeignConfig {
}
