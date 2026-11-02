package com.moamu.domain.verification.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PhotoVerificationErrorResponse(
        @NotBlank String aiTaskId,
        @NotBlank String verificationId,
        @NotBlank String processingStatus,
        @NotNull @Valid Error error) {

    public record Error(@NotBlank String code, @NotBlank String message) {
    }
}
