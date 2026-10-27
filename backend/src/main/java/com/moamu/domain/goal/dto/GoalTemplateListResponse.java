package com.moamu.domain.goal.dto;

import com.moamu.domain.goal.entity.GoalTemplate;

public record GoalTemplateListResponse(Long goalTemplateId, String goalName, String description) {
    // Goal Template -> GoalTemplateListResponse 변환 메서드
    public static GoalTemplateListResponse from(GoalTemplate goal) {
      return new GoalTemplateListResponse(goal.getGoalTemplateId(), goal.getGoalName(), goal.getDescription());
    }
} 