package com.moamu.domain.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;

import com.moamu.domain.artwork.repository.GeneratedArtworkMetadataStore;
import com.moamu.domain.challenge.exception.ChallengeErrorCode;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.repository.GoalCategoryRepository;
import com.moamu.domain.goal.repository.GoalTemplateArtworkRepository;
import com.moamu.global.exception.BusinessException;

class ReusableArtworkServiceTests {

    private final GoalCategoryRepository categories = mock(GoalCategoryRepository.class);
    private final GoalTemplateArtworkRepository artworks = mock(GoalTemplateArtworkRepository.class);
    private final GeneratedArtworkMetadataStore metadata = mock(GeneratedArtworkMetadataStore.class);
    private final ReusableArtworkService service = new ReusableArtworkService(categories, artworks, metadata);

    @Test
    void selectsAReusableArtworkForAnExistingOrNewCategoryPath() {
        when(categories.findActiveMinorId("Health", "Exercise", "Walking")).thenReturn(Optional.of(1L));
        when(artworks.findReadyArtworkIdsByCategory(1L)).thenReturn(List.of(10L, 11L));
        when(metadata.findReusableArtworkIds(List.of(10L, 11L))).thenReturn(List.of(11L));

        assertThat(service.chooseArtwork(snapshot(1L))).contains(11L);
        assertThat(service.chooseArtwork(snapshot(null))).contains(11L);
    }

    @Test
    void skipsArtworkLookupWhenCategoryDoesNotExist() {
        assertThat(service.chooseArtwork(snapshot(null))).isEmpty();
        assertThatThrownBy(() -> service.chooseArtwork(snapshot(1L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ChallengeErrorCode.ANALYSIS_STALE));
        verifyNoInteractions(artworks, metadata);
    }

    @Test
    void reportsMetadataStoreFailureAsUnavailable() {
        when(categories.findActiveMinorId("Health", "Exercise", "Walking")).thenReturn(Optional.of(1L));
        when(artworks.findReadyArtworkIdsByCategory(1L)).thenReturn(List.of(10L));
        when(metadata.findReusableArtworkIds(List.of(10L))).thenThrow(new DataRetrievalFailureException("Redis"));

        assertThatThrownBy(() -> service.chooseArtwork(snapshot(1L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE));
    }

    private GoalAnalysisSnapshot snapshot(Long categoryId) {
        return new GoalAnalysisSnapshot(1L, "Walk", "Walk daily", categoryId,
                new CategoryPath(" Health ", "Exercise", "Walking"), List.of(), null, null);
    }
}
