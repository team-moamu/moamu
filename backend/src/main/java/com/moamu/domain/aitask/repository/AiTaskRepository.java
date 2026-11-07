package com.moamu.domain.aitask.repository;

import com.moamu.domain.aitask.entity.AiTask;
import com.moamu.domain.aitask.enums.AiTaskType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiTaskRepository extends JpaRepository<AiTask, Long> {
    // 동일 접수의 재전송에는 새 작업을 만들지 않고 기존 작업을 반환
    Optional<AiTask> findByVerificationIdAndTaskType(Long verificationId, AiTaskType taskType);
}
