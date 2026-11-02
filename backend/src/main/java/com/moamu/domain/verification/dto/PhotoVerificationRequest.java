package com.moamu.domain.verification.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PhotoVerificationRequest(
        @NotBlank String aiTaskId,
        @NotBlank String verificationId,
        @NotBlank String imageKey,
        @NotEmpty List<@NotNull @Valid Condition> conditions) {

    public record Condition(@NotBlank String code, @NotBlank String description) {
    }
}
