package com.moamu.domain.goal.dto;

// 분석 결과가 만료된 뒤에도 재요청을 처리하기 위한 확정 결과
public record GoalConfirmationSnapshot(
        Long userId,
        Long challengeId) {
}
