package com.moamu.domain.goal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.moamu.domain.goal.entity.GoalTemplate;
import java.util.List;

/**
 * goalTemplateRepository
 */

public interface GoalTemplateRepository extends JpaRepository<GoalTemplate, Long> {

    @Query("""
            SELECT t FROM GoalTemplate t
            WHERE t.cateId = :categoryId AND t.goalName = :goal
                AND t.photoGuide = :photoGuide AND t.verificationScope = :scope
                AND t.status = com.moamu.domain.goal.enums.Status.ACTIVE AND t.delYn = 'N'
            ORDER BY t.goalTemplateId
            """)
    List<GoalTemplate> findReusableTemplates(
            @Param("categoryId") Long categoryId,
            @Param("goal") String goal,
            @Param("photoGuide") String photoGuide,
            @Param("scope") String scope);

    Optional<GoalTemplate> findByGoalTemplateIdAndDelYn(Long goalTemplateId, String delYn);

    List<GoalTemplate> findByDelYn(String delYn);
}