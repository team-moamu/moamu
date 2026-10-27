package com.moamu.domain.goal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moamu.domain.goal.dto.GoalTemplateListResponse;
import com.moamu.domain.goal.service.GoalTemplateService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/goalTemaplate")
@RequiredArgsConstructor
public class GoalTemplateController {
    private final GoalTemplateService goalTemplateService;

    @GetMapping
    public List<GoalTemplateListResponse> getGoalTemplates() {
        return goalTemplateService.selectGoalTemplates();
    }
}
