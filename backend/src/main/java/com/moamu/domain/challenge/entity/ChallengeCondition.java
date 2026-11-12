package com.moamu.domain.challenge.entity;

import java.time.LocalDateTime;

import com.moamu.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "challenge_condition")
@Getter 
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public class ChallengeCondition {

    // 도전 인증 조건 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_condition_id")
    private Long challengeConditionId;

    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

        // 등록 일시
    @Column(name = "frst_reg_dt", updatable = false)
    private LocalDateTime frstRegDt;

    public static ChallengeCondition create(Long challengeId, String code, String description) {
        ChallengeCondition condition = new ChallengeCondition();
        condition.challengeId = challengeId;
        condition.code = code;
        condition.description = description;
        return condition;
    }
}
