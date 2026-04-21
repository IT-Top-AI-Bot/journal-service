package com.aquadev.journalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class JournalServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(JournalServiceApplication.class, args);
    }
}
