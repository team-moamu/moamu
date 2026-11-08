package com.moamu.domain.verification.dto;

import com.moamu.domain.schedule.enums.Status;
import com.moamu.domain.verification.enums.VerificationStatus;

// 성공 화면 응답을 구성하기 위한 인증·도전·회차·작품 조회 결과
public record VerificationSuccessSnapshot(
    long verificationId,
    VerificationStatus status,
    long challengeId,
    String challengeTitle,
    long scheduleId,
    int dayNo,
    Status scheduleStatus,
    int pieceIndex,
    int gridRows,
    int gridCols,
    long artworkId,
    String artworkTitle,
    String imageKey
) {
}