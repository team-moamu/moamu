package com.moamu.domain.verification.entity;

import java.time.LocalDateTime;

import com.moamu.domain.verification.enums.AiTaskStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_task_id")
    private Long aiTaskId;

    @Column(name = "task_type", nullable = false, length = 30)
    private String taskType;

    @Column(name = "verification_id")
    private Long verificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiTaskStatus status;

    @Column(name = "task_version", nullable = false)
    private int taskVersion = 1;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    // 인증 시도에 연결할 사진 판정 AI 작업 생성
    public static AiTask createVerification(Long verificationId) {
        AiTask task = new AiTask();
        task.taskType = "VERIFY";
        task.verificationId = verificationId;
        task.status = AiTaskStatus.RUNNING;
        task.startedAt = LocalDateTime.now();
        return task;
    }

    // 기존 작업 재시도: 재시도 횟수 증가 및 이전 오류와 종료 시각 초기화
    public void retry() {
        status = AiTaskStatus.RUNNING;
        retryCount++;
        startedAt = LocalDateTime.now();
        finishedAt = null;
        lastError = null;
    }

    // AI 응답 처리 및 저장 완료
    public void complete() {
        status = AiTaskStatus.SUCCEEDED;
        finishedAt = LocalDateTime.now();
    }

    // AI 호출 또는 결과 저장 중 오류 발생 기록
    public void fail() {
        status = AiTaskStatus.FAILED_FINAL;
        lastError = "AI request failed";
        finishedAt = LocalDateTime.now();
    }
}
