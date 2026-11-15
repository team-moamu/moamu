package com.moamu.domain.goal.dto;

import java.util.List;

import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GoalEvaluationResponse(
    @NotNull Status status,
    @Valid CategoryPath proposedCategoryPath,
    @NotNull List<@NotBlank @Size(max = 300) String> conditions,
    @Size(max = 500) String photoGuide,
    @Size(max = 1000) String verificationScope,
    @NotNull List<@NotBlank String> alternatives
) {

    public enum Status {
        VERIFIABLE,
        NOT_VERIFIABLE
    }
}
