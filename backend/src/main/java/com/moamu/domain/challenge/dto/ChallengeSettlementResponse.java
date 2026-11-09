package com.moamu.domain.challenge.dto;

import java.time.LocalDate;

import com.moamu.domain.challenge.enums.Status;

// 기간 종료 처리 결과. 미확정 회차 또는 협의가 필요한 인증이 있으면 정산 대기
public record ChallengeSettlementResponse(
    Long challengeId,
    Status status,
    LocalDate endDate,
    int durationDays,
    int totalScheduleCount,
    int successScheduleCount,
    int failedScheduleCount,
    int pendingScheduleCount,
    long pendingVerificationCount,
    boolean completionAvailable
) {
}
