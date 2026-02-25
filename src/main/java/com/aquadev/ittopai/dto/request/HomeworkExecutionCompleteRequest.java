package com.aquadev.ittopai.dto.request;

import jakarta.validation.constraints.NotBlank;

public record HomeworkExecutionCompleteRequest(
        @NotBlank String s3Key
) {
}
