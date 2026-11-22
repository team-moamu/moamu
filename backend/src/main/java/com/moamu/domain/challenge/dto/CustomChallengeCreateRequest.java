package com.moamu.domain.challenge.dto;

import java.time.LocalDate;

import org.hibernate.validator.constraints.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomChallengeCreateRequest(
    @NotBlank @UUID String analysisId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate
) {
}
