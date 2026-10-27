package com.moamu.domain.goal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moamu.domain.goal.dto.GoalTemplateListResponse;
import com.moamu.domain.goal.service.GoalTemplateService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/goalTemaplate")
@RequiredArgsConstructor
public class GoalTemplateController {
    private final GoalTemplateService goalTemplateService;

    @GetMapping
    public String getMethodName(@RequestParam String param) {
        return new String();
    }
    
    public List<GoalTemplateListResponse> getGoalTemplates() {
        return goalTemplateService.selectGoalTemplates();
    }
}
