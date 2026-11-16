package com.moamu.domain.gallery.service;

import com.moamu.domain.artwork.entity.Artwork;
import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.gallery.dto.GalleryResponse;
import com.moamu.domain.gallery.dto.GalleryExhibitionResponse;
import com.moamu.domain.challenge.exception.ChallengeErrorCode;
import com.moamu.domain.gallery.repository.GalleryRepository;
import com.moamu.domain.media.entity.Media;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.schedule.enums.Status;
import com.moamu.domain.verification.repository.VerificationSuccessRepository;
import com.moamu.domain.verification.service.ArtworkImageUrlService;
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
public class GalleryService {

    private final GalleryRepository galleryRepository;
    private final VerificationSuccessRepository verificationSuccessRepository;
    private final ArtworkRepository artworkRepository;
    private final MediaRepository mediaRepository;
    private final ArtworkImageUrlService artworkImageUrlService;

    @Transactional
    public GalleryExhibitionResponse hideExhibition(Long userId, Long challengeId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (challengeId == null || challengeId <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        Challenge challenge = galleryRepository.findOwnedForExhibition(userId, challengeId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        if (challenge.getStatus() != com.moamu.domain.challenge.enums.Status.COMPLETED) {
            throw new BusinessException(ChallengeErrorCode.CHALLENGE_NOT_COMPLETED);
        }
        challenge.setIsExhibited('N');

        return new GalleryExhibitionResponse(
            challenge.getChallengeId(), challenge.getIsExhibited() == 'Y'
        );
    }

    public List<GalleryResponse> getGalleries(Long userId, String sort) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        // latest는 최신순, name은 이름순, successRate는 성공률순
        if (!"latest".equals(sort) && !"name".equals(sort) && !"successRate".equals(sort)) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        List<Challenge> challenges = galleryRepository.findExhibited(userId, sort);
        List<GalleryResponse> responses = new ArrayList<>();

        for (Challenge challenge : challenges) {
            // 1. 도전 기간 및 성공 현황 생성
            List<Integer> revealedPieceIndices = verificationSuccessRepository
                .findRevealed(challenge.getChallengeId(), Status.SUCCESS);

            int durationDays = challenge.getDurationDays();
            int successDays = revealedPieceIndices.size();
            int progressPercent = durationDays > 0
                ? (int) Math.round(successDays * 100.0 / durationDays)
                : 0;

            GalleryResponse.Progress progress = new GalleryResponse.Progress(
                durationDays,
                successDays,
                progressPercent
            );

            // 2. 작품 정보 생성
            Artwork artwork = artworkRepository
                .findByArtworkIdAndDelYn(challenge.getArtworkId(), "N")
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

            
            // 작품에 연결된 미디어가 없으면 이미지 키는 null로 변환
            String imageKey = artwork.getMediaId() == null
                ? null
                : mediaRepository.findById(artwork.getMediaId())
                    .map(Media::getImageKey)
                    .orElse(null);

            GalleryResponse.Artworks artworkResponse = new GalleryResponse.Artworks(
                artwork.getArtworkId(),
                artwork.getArtworkTitle(),
                artworkImageUrlService.resolve(imageKey)
            );

            // 3. 퍼즐 상태 연결
            GalleryResponse.Puzzle puzzle = new GalleryResponse.Puzzle(
                challenge.getGridRows(),
                challenge.getGridCols(),
                revealedPieceIndices,
                successDays,
                durationDays
            );

            // 4. 객체들 응답에 담기
            responses.add(new GalleryResponse(
                challenge.getChallengeId(),
                challenge.getChallengeTitle(),
                challenge.getGoalText(),
                challenge.getStartDate(),
                challenge.getEndDate(),
                progress,
                artworkResponse,
                puzzle
            ));
        }

        return responses;
    }
}
