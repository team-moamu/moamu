package com.moamu.domain.goal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moamu.domain.goal.entity.GoalTemplateCondition;
import java.util.List;


/**
 * GoalTemplateConditionRepository
 */
public interface GoalTemplateConditionRepository extends JpaRepository<GoalTemplateCondition, Long> {
    List<GoalTemplateCondition> findByGoalTemplateIdAndDelYn(Long goalTemplateId, String delYn);
    
}