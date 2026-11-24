package com.moamu.domain.artwork.dto;

import java.time.Instant;
import java.util.List;

import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;

public record ArtworkGenerationRequest(
        long taskId,
        int taskVersion,
        Instant expiresAt,
        String normalizedGoal,
        CategoryPath categoryPath,
        List<String> conditions,
        String photoGuide) {
}
