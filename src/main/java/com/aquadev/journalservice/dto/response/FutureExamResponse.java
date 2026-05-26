package com.aquadev.journalservice.dto.response;

import java.time.LocalDate;

public record FutureExamResponse(
        String spec,
        LocalDate date
) {
}
