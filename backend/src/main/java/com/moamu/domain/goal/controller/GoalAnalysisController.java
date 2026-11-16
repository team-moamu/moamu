package com.moamu.domain.goal.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moamu.domain.goal.dto.GoalAnalysisRequest;
import com.moamu.domain.goal.dto.GoalAnalysisResponse;
import com.moamu.domain.goal.service.GoalAnalysisService;
import com.moamu.global.response.ApiResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalAnalysisController {

    private final GoalAnalysisService goalAnalysisService;

    // 자유목표 분석 요청
    @PostMapping("/analyze")
    public ApiResponse<GoalAnalysisResponse> analyze(@AuthenticationPrincipal Long userId, @Valid @RequestBody GoalAnalysisRequest request) {
        GoalAnalysisResponse response = goalAnalysisService.analyze(userId, request);
        return ApiResponse.success(response);
    }
}
