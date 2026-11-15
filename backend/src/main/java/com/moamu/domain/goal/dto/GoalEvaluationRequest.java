package com.moamu.domain.goal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record GoalEvaluationRequest(@NotBlank @Size(max = 500) String goal, @Valid CategoryPath categoryPath) {

    public record CategoryPath(
            @NotBlank @Size(max = 50) String major,
            @NotBlank @Size(max = 50) String middle,
            @NotBlank @Size(max = 50) String minor) {

    }
}
