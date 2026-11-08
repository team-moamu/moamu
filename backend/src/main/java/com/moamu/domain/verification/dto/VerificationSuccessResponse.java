package com.moamu.domain.verification.dto;

import java.util.List;

import com.moamu.domain.verification.enums.CheckStatus;

// 인증 성공 화면의 작품, 퍼즐 진행도 및 조건별 판정 정보
public record VerificationSuccessResponse(
    long verificationId,
    String status,
    long challengeId,
    String challengeTitle,
    long scheduleId,
    int dayNo,
    String scheduleStatus,
    Artwork artwork,
    int gridRows,
    int gridCols,
    // 이번 인증 회차에 미리 배정된 0 기반 조각 위치
    int openedPieceIndex,
    List<Integer> revealedPieceIndices,
    int earnedPieceCount,
    // 격자 칸 수가 아닌 도전의 전체 회차 수
    int totalPieceCount,
    List<Check> checks
) {
    // 작품 이미지 URL은 유효 시간이 있는 S3 서명 URL이며 미디어가 없으면 null
    public record Artwork(
        long artworkId,
        String title,
        String imageUrl
    ) {
    }

    // 도전 생성 당시 저장된 인증 조건과 이번 시도의 판정 결과
    public record Check(
        long conditionId,
        String code,
        String description,
        CheckStatus status,
        String reason
    ) {
    }
}