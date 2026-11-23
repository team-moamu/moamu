package com.moamu.domain.challenge.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.moamu.domain.artwork.repository.GeneratedArtworkMetadataStore;
import com.moamu.domain.challenge.exception.ChallengeErrorCode;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.repository.GoalCategoryRepository;
import com.moamu.domain.goal.repository.GoalTemplateArtworkRepository;
import com.moamu.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReusableArtworkService {

    private final GoalCategoryRepository goalCategoryRepository;
    private final GoalTemplateArtworkRepository goalTemplateArtworkRepository;
    private final GeneratedArtworkMetadataStore artworkMetadataStore;

    public Optional<Long> chooseArtwork(GoalAnalysisSnapshot snapshot) {
        var path = snapshot.categoryPath();
        Optional<Long> activeCategoryId = goalCategoryRepository.findActiveMinorId(
                path.major().strip(), path.middle().strip(), path.minor().strip());
        // 분석 때 사용한 소분류가 바뀌거나 비활성화됐는지 확인한다.
        if (snapshot.categoryId() != null && !snapshot.categoryId().equals(activeCategoryId.orElse(null))) {
            throw new BusinessException(ChallengeErrorCode.ANALYSIS_STALE);
        }
        if (activeCategoryId.isEmpty()) {
            return Optional.empty();
        }

        List<Long> artworkIds = goalTemplateArtworkRepository.findReadyArtworkIdsByCategory(activeCategoryId.get());
        try {
            List<Long> reusableIds = artworkMetadataStore.findReusableArtworkIds(artworkIds);
            if (reusableIds.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(reusableIds.get(ThreadLocalRandom.current().nextInt(reusableIds.size())));
        } catch (DataAccessException exception) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE, exception);
        }
    }
}
