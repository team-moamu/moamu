package com.moamu.domain.gallery.dto;

import java.time.LocalDate;
import java.util.List;

// 미술관의 작품 한 건에 대한 응답 DTO
public record GalleryResponse (
    Long challengeId,
    String challengeTitle,
    LocalDate endDate,
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
        String imageUrl
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
