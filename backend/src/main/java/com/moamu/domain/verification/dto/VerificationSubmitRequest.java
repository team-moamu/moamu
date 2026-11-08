package com.moamu.domain.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VerificationSubmitRequest(
        @NotNull @Positive Long challengeId,
        @NotNull @Positive Long scheduleId,
        @NotNull @Positive Long mediaId,
        @NotBlank @Size(max = 100) String idempotencyKey) {
}
