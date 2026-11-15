package com.moamu.domain.goal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record GoalEvaluationRequest(@NotBlank String goal, @Valid CategoryPath categoryPath) {

    public record CategoryPath(@NotBlank String major, @NotBlank String middle, @NotBlank String minor) {
        
    }
}
