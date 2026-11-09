package com.moamu.domain.challenge.dto;

import java.time.LocalDate;
import java.util.List;

import com.moamu.domain.challenge.enums.Status;

// 도전 전체 완료 화면용 응답. 일일 인증 결과와 구분하며 전시 여부는 변경하지 않음
public record ChallengeCompletionResponse(
    Long challengeId,
    String challengeTitle,
    Status status,
    LocalDate startDate,
    LocalDate endDate,
    int durationDays,
    Artwork artwork,
    int gridRows,
    int gridCols,
    // 성공한 회차에 미리 배정된 0 기반 조각 위치. 미획득 위치는 화면에서 검정으로 표시
    List<Integer> revealedPieceIndices,
    // 격자 칸 수가 아닌 도전 전체 회차 수
    int totalPieceCount,
    int earnedPieceCount,
    int missingPieceCount,
    // true이면 회차에 배정되지 않은 격자 칸도 가리지 않고 작품 전체 표시
    boolean allPiecesEarned
) {
    public record Artwork(
        Long artworkId,
        String title,
        // 15분 유효한 S3 서명 URL. 작품 미디어가 없으면 null
        String imageUrl
    ) {
    }
}
