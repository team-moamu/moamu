package com.moamu.domain.goal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moamu.domain.goal.dto.GoalTemplateListResponse;
import com.moamu.domain.goal.service.GoalTemplateService;
import com.moamu.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/goalTemaplate")
@RequiredArgsConstructor
public class GoalTemplateController {
    private final GoalTemplateService goalTemplateService;

    @GetMapping
    public ApiResponse<List<GoalTemplateListResponse>> getGoalTemplates() {
        List<GoalTemplateListResponse> goalTemplates = goalTemplateService.selectGoalTemplates();
        return ApiResponse.success(goalTemplates);
    }
}
