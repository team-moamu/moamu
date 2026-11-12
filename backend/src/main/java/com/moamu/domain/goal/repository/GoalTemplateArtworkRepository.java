package com.moamu.domain.goal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moamu.domain.goal.entity.GoalTemplateArtwork;

public interface GoalTemplateArtworkRepository extends JpaRepository<GoalTemplateArtwork, Long> {

    Optional<GoalTemplateArtwork> findByGoalTemplateId(Long goalTemplateId);

    Optional<GoalTemplateArtwork> findByGoalTemplateIdAndDelYn(Long goalTemplateId, String delYn);
}
