package com.moamu.domain.collection.service;

import com.moamu.domain.artwork.entity.Artwork;
import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.collection.dto.CollectionResponse;
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

    public List<CollectionResponse> getCollections(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        List<Challenge> challenges = collectionRepository.findOwned(userId);
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
                challenge.getIsExhibited() == 'Y',
                progressResponse,
                artworkResponse,
                puzzle
            ));
        }

        return responses;
    }
}
