package com.moamu.domain.challenge.repository;

import java.util.List;
import com.moamu.domain.challenge.entity.ChallengeCondition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeConditionRepository extends JpaRepository<ChallengeCondition, Long> {
    List<ChallengeCondition> findByChallengeIdOrderByChallengeConditionIdAsc(Long challengeId);
}
