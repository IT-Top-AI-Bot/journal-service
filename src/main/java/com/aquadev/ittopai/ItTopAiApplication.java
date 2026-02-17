package com.aquadev.ittopai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ItTopAiApplication {

    private ItTopAiApplication() {
    }

    static void main(String[] args) {
        SpringApplication.run(ItTopAiApplication.class, args);
    }
}
