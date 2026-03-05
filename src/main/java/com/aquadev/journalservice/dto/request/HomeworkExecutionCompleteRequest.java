package com.aquadev.journalservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record HomeworkExecutionCompleteRequest(
        @NotBlank String s3Key
) {
}
