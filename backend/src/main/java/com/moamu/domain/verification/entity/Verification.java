package com.moamu.domain.verification.entity;

import java.time.LocalDateTime;

import com.moamu.domain.verification.enums.VerificationResult;
import com.moamu.domain.verification.enums.VerificationStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long verificationId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "file_hash", nullable = false, columnDefinition = "CHAR(64)")
    private String fileHash;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(length = 500)
    private String reason;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "upd_dt")
    private LocalDateTime updDt;

    // 새 인증 시도 생성. 중복 제출 방지 키와 업로드 사진 정보를 함께 저장
    public static Verification create(Long scheduleId, int attemptNo, String idempotencyKey,
            String fileHash, Long mediaId) {
        Verification verification = new Verification();
        verification.scheduleId = scheduleId;
        verification.attemptNo = attemptNo;
        verification.idempotencyKey = idempotencyKey;
        verification.fileHash = fileHash;
        verification.mediaId = mediaId;
        verification.status = VerificationStatus.ANALYZING;
        verification.submittedAt = LocalDateTime.now();
        return verification;
    }

    // 기술 오류가 발생한 기존 인증 시도를 다시 분석 상태로 변경
    public void retry() {
        status = VerificationStatus.ANALYZING;
        updDt = LocalDateTime.now();
    }

    // 최종 판정, 모델 버전, 종합 사유 및 분석 완료 시각 반영
    public void complete(VerificationResult result, String modelVersion, String reason) {
        status = VerificationStatus.valueOf(result.name());
        this.modelVersion = modelVersion;
        this.reason = reason;
        analyzedAt = LocalDateTime.now();
        updDt = analyzedAt;
    }

    // 인증 조건 미충족이 아닌 통신 또는 저장 오류로 기록
    public void fail() {
        status = VerificationStatus.TECH_ERROR;
        updDt = LocalDateTime.now();
    }
}
