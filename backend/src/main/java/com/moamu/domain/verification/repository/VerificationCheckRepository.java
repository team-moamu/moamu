package com.moamu.domain.verification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moamu.domain.verification.entity.VerificationCheck;

public interface VerificationCheckRepository extends JpaRepository<VerificationCheck, Long> {
    List<VerificationCheck> findByVerificationIdOrderByChallengeConditionIdAsc(Long verificationId);
}
