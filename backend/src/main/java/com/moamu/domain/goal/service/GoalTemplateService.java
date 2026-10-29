package com.moamu.domain.goal.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moamu.domain.goal.dto.GoalTemplateListResponse;
import com.moamu.domain.goal.entity.GoalTemplate;
import com.moamu.domain.goal.repository.GoalTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class GoalTemplateService {

    private final GoalTemplateRepository goalTemplateRepository;

    public List<GoalTemplateListResponse> selectGoalTemplates() {
        List<GoalTemplate> goals = goalTemplateRepository.findByDelYn("N");

        // 반환타입 변환
        List<GoalTemplateListResponse> result = new ArrayList<>();

        for(GoalTemplate g : goals) {
            result.add(GoalTemplateListResponse.from(g));
        }
        return result;
    }
}
