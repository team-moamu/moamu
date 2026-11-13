package com.moamu.domain.collection.service;

import com.moamu.domain.artwork.entity.Artwork;
import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.challenge.exception.ChallengeErrorCode;
import com.moamu.domain.collection.dto.CollectionResponse;
import com.moamu.domain.collection.dto.CollectionExhibitionResponse;
import com.moamu.domain.collection.repository.CollectionRepository;
import com.moamu.domain.media.entity.Media;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.schedule.enums.Status;
import com.moamu.domain.verification.repository.VerificationSuccessRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final VerificationSuccessRepository verificationSuccessRepository;
    private final ArtworkRepository artworkRepository;
    private final MediaRepository mediaRepository;

    // 소유권과 완료 여부를 확인하고 전시 상태를 Y로 변경
    @Transactional
    public CollectionExhibitionResponse exhibit(Long userId, Long challengeId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (challengeId == null || challengeId <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        Challenge challenge = collectionRepository.findOwnedForExhibition(userId, challengeId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        if (challenge.getStatus() != com.moamu.domain.challenge.enums.Status.COMPLETED) {
            throw new BusinessException(ChallengeErrorCode.CHALLENGE_NOT_COMPLETED);
        }
        challenge.setIsExhibited('Y');

        return new CollectionExhibitionResponse(
            challenge.getChallengeId(), challenge.getIsExhibited() == 'Y'
        );
    }

    public List<CollectionResponse> getCollections(Long userId, Boolean exhibited, String sort) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        // 정렬 기본값(latest)은 최신순, name 이름순, successRate 성공률순
        if (!"latest".equals(sort) && !"name".equals(sort) && !"successRate".equals(sort)) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        // 필터 생략 시 전체
        // true는 전시 중(Y)
        // false는 미전시(N) 조회
        Character exhibitionStatus = exhibited == null ? null : (exhibited ? 'Y' : 'N');
        List<Challenge> challenges = collectionRepository.findOwned(userId, exhibitionStatus, sort);
        List<CollectionResponse> responses = new ArrayList<>();

        for (Challenge challenge : challenges) {
            // 1. 도전 기간 및 성공 현황 생성
            List<Integer> revealedPieceIndices = verificationSuccessRepository
                .findRevealed(challenge.getChallengeId(), Status.SUCCESS);

            int durationDays = challenge.getDurationDays();
            int successDays = revealedPieceIndices.size();
            int progressPercent = durationDays > 0
                ? (int) Math.round(successDays * 100.0 / durationDays)
                : 0;

            CollectionResponse.Progress progressResponse = new CollectionResponse.Progress(
                durationDays,
                successDays,
                progressPercent
            );

            // 2. 작품 정보 생성
            Artwork artwork = artworkRepository
                .findByArtworkIdAndDelYn(challenge.getArtworkId(), "N")
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

            // 작품에 연결된 미디어가 없으면 이미지 키는 null로 반환
            String imageKey = artwork.getMediaId() == null
                ? null
                : mediaRepository.findById(artwork.getMediaId())
                    .map(Media::getImageKey)
                    .orElse(null);

            CollectionResponse.Artworks artworkResponse =
                new CollectionResponse.Artworks(
                    artwork.getArtworkId(),
                    artwork.getArtworkTitle(),
                    imageKey
                );

            // 3. 퍼즐 상태 생성
            CollectionResponse.Puzzle puzzle =
                new CollectionResponse.Puzzle(
                    challenge.getGridRows(),
                    challenge.getGridCols(),
                    revealedPieceIndices,
                    revealedPieceIndices.size(),
                    durationDays // 하루에 한 조각을 배정하는 규칙 기준
                );

            // 4. 객체들 응답에 담기
            responses.add(new CollectionResponse(
                challenge.getChallengeId(),
                challenge.getChallengeTitle(),
                challenge.getStatus(),
                challenge.getEndDate(),
                challenge.getIsExhibited() == 'Y',
                progressResponse,
                artworkResponse,
                puzzle
            ));
        }

        return responses;
    }
}
