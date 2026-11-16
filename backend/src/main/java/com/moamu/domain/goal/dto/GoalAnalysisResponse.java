package com.moamu.domain.goal.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moamu.domain.goal.dto.GoalEvaluationResponse.Status;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoalAnalysisResponse(
        Status status,
        String analysisId,
        String goal,
        String normalizedGoal,
        List<String> conditions,
        String photoGuide,
        String verificationScope,
        List<String> alternatives) {

    public static GoalAnalysisResponse verifiable(String analysisId, String goal, GoalEvaluationResponse result) {
        return new GoalAnalysisResponse(
                Status.VERIFIABLE,
                analysisId,
                goal,
                result.normalizedGoal(),
                result.conditions(),
                result.photoGuide(),
                result.verificationScope(),
                null);
    }

    public static GoalAnalysisResponse notVerifiable(List<String> alternatives) {
        return new GoalAnalysisResponse(
                Status.NOT_VERIFIABLE,
                null,
                null,
                null,
                null,
                null,
                null,
                alternatives);
    }
}
