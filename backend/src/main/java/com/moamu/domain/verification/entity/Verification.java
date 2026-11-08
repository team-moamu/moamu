package com.moamu.domain.verification.entity;

import com.moamu.domain.verification.enums.VerificationStatus;
import com.moamu.domain.verification.enums.VerificationResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification {

    // 사진 제출 식별 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long verificationId;

    // 도전 회차 ID
    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    // media ID
    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    // 해당 회차 제출 순번
    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    // 동일 제출 요청의 중복 생성을 방지하기 위해 저장하는 멱등성 키
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    // 제출 사진 SHA-256 해시
    @Column(name = "file_hash", nullable = false, columnDefinition = "CHAR(64)")
    private String fileHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VerificationStatus status;

    // 인증 사진 제출 시각
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    // 인증 시도 레코드 생성 시각
    @Column(name = "frst_reg_dt", updatable = false)
    private LocalDateTime frstRegDt;

    // 기존 인증 결과 저장 흐름에서 사용하는 판정 정보
    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(length = 500)
    private String reason;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "upd_dt")
    private LocalDateTime updDt;

    // 기존 호출부가 사용하는 분석 시작 상태의 인증 생성
    public static Verification create(Long scheduleId, int attemptNo, String idempotencyKey,
            String fileHash, Long mediaId) {
        Verification verification = submitted(scheduleId, mediaId, attemptNo,
                idempotencyKey, fileHash, LocalDateTime.now());
        verification.status = VerificationStatus.ANALYZING;
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

    public static Verification submitted(Long scheduleId, Long mediaId, int attemptNo,
            String idempotencyKey, String fileHash, LocalDateTime now) {
        Verification verification = new Verification();
        verification.scheduleId = scheduleId;
        verification.mediaId = mediaId;
        verification.attemptNo = attemptNo;
        verification.idempotencyKey = idempotencyKey;
        verification.fileHash = fileHash;
        verification.status = VerificationStatus.SUBMITTED;
        verification.submittedAt = now;
        verification.frstRegDt = now;
        return verification;
    }

}
