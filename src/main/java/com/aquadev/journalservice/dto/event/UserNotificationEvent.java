package com.aquadev.journalservice.dto.event;

public record UserNotificationEvent(Long telegramId, NotificationType type) {
}
