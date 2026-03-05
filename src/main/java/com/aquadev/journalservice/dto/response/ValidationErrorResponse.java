package com.aquadev.journalservice.dto.response;

public record ValidationErrorResponse(
        String field,
        String message
) {
}
