package com.moamu.domain.goal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.moamu.domain.goal.entity.GoalTemplate;

/**
 * goalTemplateRepository
 */

public interface GoalTemplateRepository extends JpaRepository<GoalTemplate, Long> {
    Optional<GoalTemplate> findByGoalTemplateIdAndDelYn(Long goalTemplateId, String delYn);
}