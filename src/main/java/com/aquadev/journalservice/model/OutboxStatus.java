package com.aquadev.journalservice.model;

public enum OutboxStatus {
    NEW,
    PROCESSING,
    SENT,
    ERROR
}
