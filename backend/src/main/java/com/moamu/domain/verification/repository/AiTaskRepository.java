package com.moamu.domain.verification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moamu.domain.verification.entity.AiTask;

public interface AiTaskRepository extends JpaRepository<AiTask, Long> {
    Optional<AiTask> findFirstByVerificationIdOrderByAiTaskIdDesc(Long verificationId);
}
