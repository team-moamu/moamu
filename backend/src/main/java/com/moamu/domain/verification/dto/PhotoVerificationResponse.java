package com.moamu.domain.verification.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.moamu.domain.verification.enums.CheckStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PhotoVerificationResponse(
        @NotBlank String aiTaskId,
        @NotBlank String verificationId,
        @NotBlank String processingStatus,
        @NotEmpty List<@NotNull @Valid Check> checks,
        @NotBlank @Size(max = 50) String modelVersion) {

    public record Check(
            @NotBlank String code,
            @NotNull CheckStatus status,
            @NotBlank @Size(max = 500) String reason) {
    }
}
