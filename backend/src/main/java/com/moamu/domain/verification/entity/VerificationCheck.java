package com.moamu.domain.verification.entity;

import com.moamu.domain.verification.enums.CheckStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "verification_check")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_check_id")
    private Long verificationCheckId;

    @Column(name = "verification_id", nullable = false)
    private Long verificationId;

    @Column(name = "challenge_condition_id", nullable = false)
    private Long challengeConditionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckStatus status;

    @Column(length = 500)
    private String reason;

    // 인증 조건 하나에 대한 AI 판정 상태와 사유 생성
    public static VerificationCheck create(Long verificationId, Long challengeConditionId,
            CheckStatus status, String reason) {
        VerificationCheck check = new VerificationCheck();
        check.verificationId = verificationId;
        check.challengeConditionId = challengeConditionId;
        check.status = status;
        check.reason = reason;
        return check;
    }
}
