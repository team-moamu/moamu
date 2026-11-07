package com.moamu.domain.verification.entity;

import com.moamu.domain.verification.enums.VerificationStatus;
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

    // 인증 시도 처리 상태
    @Column(name = "status", nullable = false, length = 20)
    private VerificationStatus status;

    // 인증 사진 제출 시각
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    // 인증 시도 레코드 생성 시각
    @Column(name = "frst_reg_dt", updatable = false)
    private LocalDateTime frstRegDt;

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
