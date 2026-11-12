package com.moamu.domain.collection.dto;

import com.moamu.domain.challenge.enums.Status;

import java.util.List;

// 보관함의 완료된 도전 한 건에 대한 응답 DTO
public record CollectionResponse(
    Long challengeId,
    String challengeTitle,
    Status challengeStatus,
    boolean isExhibited,
    Progress progress,
    Artworks artwork,
    Puzzle puzzle
){
    // 도전 기간 및 성공 현황
    public record Progress(
        int durationDays,
        int successDays,
        int progressPercent
    ){}

    // 작품 정보
    public record Artworks(
        Long artworkId,
        String artworkTitle,
        String imageKey
    ){}

    // 퍼즐 상태 정보
    public record Puzzle(
        int gridRows,
        int gridCols,
        List<Integer> revealedPieceIndices,
        int earnedPieceCount,
        int totalPieceCount
    ){}

}
