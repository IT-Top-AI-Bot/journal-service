package com.aquadev.ittopai.dto.response;

public record ValidationErrorResponse(
        String field,
        String message
) {
}
