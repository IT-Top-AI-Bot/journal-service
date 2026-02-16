package com.aquadev.ittopai.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path,
        List<ValidationErrorResponse> errors
) {
}
