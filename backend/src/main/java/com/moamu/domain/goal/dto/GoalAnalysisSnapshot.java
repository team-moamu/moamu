package com.moamu.domain.goal.dto;

import java.util.List;

import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;

// 도전 확정 시 사용할 사용자별 분석 결과. 기존 경로도 확정 시 활성 여부를 다시 확인.
public record GoalAnalysisSnapshot(
        Long userId,
        String goal,
        String normalizedGoal,
        Long categoryId,
        CategoryPath categoryPath,
        List<String> conditions,
        String photoGuide,
        String verificationScope) {
}
