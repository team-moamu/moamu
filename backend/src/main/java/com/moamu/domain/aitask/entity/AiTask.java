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

    // AI 작업 실행 시작 시각
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    // AI 작업 종료 시각
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // 마지막 기술 오류 내용
    @Column(name = "last_error", length = 1000)
    private String lastError;

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

    // 기존 인증 흐름에서 사용하는 실행 중인 사진 판정 작업 생성
    public static AiTask createVerification(Long verificationId) {
        AiTask task = pendingVerification(verificationId, LocalDateTime.now());
        task.status = AiTaskStatus.RUNNING;
        task.startedAt = task.frstRegDt;
        return task;
    }

    // 기존 작업 재시도: 횟수 증가 및 이전 오류와 종료 시각 초기화
    public void retry() {
        status = AiTaskStatus.RUNNING;
        retryCount++;
        startedAt = LocalDateTime.now();
        finishedAt = null;
        lastError = null;
    }

    // AI 응답 처리 및 결과 저장 완료
    public void complete() {
        status = AiTaskStatus.SUCCEEDED;
        finishedAt = LocalDateTime.now();
    }

    // AI 호출 또는 결과 저장 중 기술 오류 기록
    public void fail() {
        status = AiTaskStatus.FAILED_FINAL;
        lastError = "AI request failed";
        finishedAt = LocalDateTime.now();
    }
}
