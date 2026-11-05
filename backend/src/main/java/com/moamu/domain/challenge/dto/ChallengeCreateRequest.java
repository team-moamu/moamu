package com.moamu.domain.challenge.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ChallengeCreateRequest(
    @NotNull 
    Long goalTemplateId,

    @NotNull 
    Integer durationDays,

    @NotNull 
    LocalDate startDate
) {}