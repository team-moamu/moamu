package com.moamu.domain.aitask.entity;

import com.moamu.domain.aitask.enums.AiTaskStatus;
import com.moamu.domain.aitask.enums.AiTaskType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// AI 호출 전 저장
@Entity
@Table(name = "ai_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTask {
    // AI 서버 요청에 사용할 작업 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_task_id")
    private Long aiTaskId;

    // 작업 종류
    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private AiTaskType taskType;

    // 인증 시도 ID
    @Column(name = "verification_id")
    private Long verificationId;

    // 작업 요청 버전
    @Column(name = "task_version", nullable = false)
    private int taskVersion;

    // AI 작업의 처리 상태 (기본값: 실행 대기)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiTaskStatus status = AiTaskStatus.PENDING;

    // AI의 기술적 재시도 횟수
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    // 작업 최초 등록 시각
    @Column(name = "frst_reg_dt", updatable = false)
    private LocalDateTime frstRegDt;

    // 저장된 인증 시도에 연결할 사진 판정 대기 작업을 생성
    public static AiTask pendingVerification(Long verificationId, LocalDateTime now) {
        AiTask task = new AiTask();
        task.taskType = AiTaskType.VERIFY;
        task.verificationId = verificationId;
        task.taskVersion = 1;
        task.retryCount = 0;
        task.frstRegDt = now;
        return task;
    }
}
