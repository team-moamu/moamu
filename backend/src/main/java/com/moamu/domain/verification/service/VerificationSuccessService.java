package com.moamu.domain.verification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.moamu.domain.schedule.enums.Status;
import com.moamu.domain.verification.dto.VerificationSuccessResponse;
import com.moamu.domain.verification.dto.VerificationSuccessResponse.Artwork;
import com.moamu.domain.verification.dto.VerificationSuccessResponse.Check;
import com.moamu.domain.verification.dto.VerificationSuccessSnapshot;
import com.moamu.domain.verification.enums.VerificationStatus;
import com.moamu.domain.verification.repository.VerificationSuccessRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationSuccessService {

    private final VerificationSuccessRepository verificationSuccessRepository;
    private final ArtworkImageUrlService artworkImageUrlService;

    // 공개 목록과 진행도를 동일한 조회 시점 기준으로 구성하며 DB 상태는 변경하지 않음
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public VerificationSuccessResponse getSuccess(Long userId, long verificationId) {
        // 로그인 사용자와 인증 ID 검증
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (verificationId <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        // 소유자의 인증 정보 조회: 삭제된 도전과 다른 사용자의 인증은 제외
        VerificationSuccessSnapshot snapshot = verificationSuccessRepository
            .findOwned(userId, verificationId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        // 성공 화면 진입은 회차 상태가 아닌 인증 성공 여부로 판단
        if (snapshot.status() != VerificationStatus.SUCCESS) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        // 공개 조각 목록과 전체 회차 수로 진행도 계산
        List<Integer> revealedPieceIndices = verificationSuccessRepository
            .findRevealed(snapshot.challengeId(), Status.SUCCESS);
        int earnedPieceCount = revealedPieceIndices.size();
        int totalPieceCount = Math.toIntExact(verificationSuccessRepository.countPieces(snapshot.challengeId()));

        // 작품 이미지 URL과 해당 인증 시도의 조건별 결과 조회
        String imageUrl = artworkImageUrlService.resolve(snapshot.imageKey());
        Artwork artwork = new Artwork(snapshot.artworkId(), snapshot.artworkTitle(), imageUrl);
        List<Check> checks = verificationSuccessRepository.findChecks(verificationId);

        // 이번 회차에 미리 배정된 조각과 현재 공개 상태를 응답으로 구성
        return new VerificationSuccessResponse(
            snapshot.verificationId(),
            snapshot.status().name(),
            snapshot.challengeId(),
            snapshot.challengeTitle(),
            snapshot.scheduleId(),
            snapshot.dayNo(),
            snapshot.scheduleStatus().name(),
            artwork,
            snapshot.gridRows(),
            snapshot.gridCols(),
            snapshot.pieceIndex(),
            revealedPieceIndices,
            earnedPieceCount,
            totalPieceCount,
            checks
        );
    }
}