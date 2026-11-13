package com.moamu.domain.collection;

import com.moamu.domain.artwork.entity.Artwork;
import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.collection.controller.CollectionController;
import com.moamu.domain.collection.repository.CollectionRepository;
import com.moamu.domain.collection.service.CollectionService;
import com.moamu.domain.media.entity.Media;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.schedule.enums.Status;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.domain.verification.repository.VerificationSuccessRepository;
import com.moamu.global.config.SecurityConfig;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import com.moamu.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 실제 보안 필터·컨트롤러·서비스를 연결한다. DB 조회와 JWT 해석은 대역으로 격리한다.
@WebMvcTest(CollectionController.class)
@Import({SecurityConfig.class, CollectionService.class})
class CollectionApiTests {
    @Autowired MockMvc mvc;
    @Autowired CollectionService service;
    @MockitoBean CollectionRepository collections;
    @MockitoBean VerificationSuccessRepository verifications;
    @MockitoBean ArtworkRepository artworks;
    @MockitoBean MediaRepository media;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean UserRepository users;

    private void authenticate() {
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(tokens.getUserId("owner-token")).thenReturn(1L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
    }

    private Challenge challenge(long id, int duration, char exhibited) {
        Challenge challenge = mock(Challenge.class);
        when(challenge.getChallengeId()).thenReturn(id);
        when(challenge.getChallengeTitle()).thenReturn("Practice guitar " + id);
        when(challenge.getStatus()).thenReturn(com.moamu.domain.challenge.enums.Status.COMPLETED);
        when(challenge.getDurationDays()).thenReturn(duration);
        when(challenge.getEndDate()).thenReturn(LocalDate.of(2026, 11, 10));
        when(challenge.getIsExhibited()).thenReturn(exhibited);
        when(challenge.getArtworkId()).thenReturn(30L);
        when(challenge.getGridRows()).thenReturn(3);
        when(challenge.getGridCols()).thenReturn(3);
        return challenge;
    }

    private Artwork artwork(Long mediaId) {
        Artwork artwork = mock(Artwork.class);
        when(artwork.getArtworkId()).thenReturn(30L);
        when(artwork.getArtworkTitle()).thenReturn("Painting");
        when(artwork.getMediaId()).thenReturn(mediaId);
        when(artworks.findByArtworkIdAndDelYn(30L, "N")).thenReturn(Optional.of(artwork));
        return artwork;
    }

    @Test
    void returnsCollectionWithoutUserIdParameter() throws Exception {
        authenticate();
        doReturn(List.of(challenge(10, 7, 'Y'))).when(collections).findOwned(1L, null);
        when(verifications.findRevealed(10, Status.SUCCESS)).thenReturn(List.of(0, 2, 6));
        artwork(60L);
        Media image = mock(Media.class);
        when(image.getImageKey()).thenReturn("artworks/test.png");
        when(media.findById(60L)).thenReturn(Optional.of(image));

        mvc.perform(get("/api/collections").header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].challengeId").value(10))
            .andExpect(jsonPath("$.data[0].challengeTitle").value("Practice guitar 10"))
            .andExpect(jsonPath("$.data[0].challengeStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.data[0].endDate").value("2026-11-10"))
            .andExpect(jsonPath("$.data[0].isExhibited").value(true))
            .andExpect(jsonPath("$.data[0].progress.durationDays").value(7))
            .andExpect(jsonPath("$.data[0].progress.successDays").value(3))
            .andExpect(jsonPath("$.data[0].progress.progressPercent").value(43))
            .andExpect(jsonPath("$.data[0].artwork.artworkId").value(30))
            .andExpect(jsonPath("$.data[0].artwork.artworkTitle").value("Painting"))
            .andExpect(jsonPath("$.data[0].artwork.imageKey").value("artworks/test.png"))
            .andExpect(jsonPath("$.data[0].puzzle.gridRows").value(3))
            .andExpect(jsonPath("$.data[0].puzzle.gridCols").value(3))
            .andExpect(jsonPath("$.data[0].puzzle.revealedPieceIndices").value(contains(0, 2, 6)))
            .andExpect(jsonPath("$.data[0].puzzle.earnedPieceCount").value(3))
            .andExpect(jsonPath("$.data[0].puzzle.totalPieceCount").value(7));
    }

    @ParameterizedTest
    @CsvSource({"true,Y", "false,N"})
    void filtersByExhibition(boolean exhibited, char storedStatus) throws Exception {
        authenticate();
        doReturn(List.of(challenge(10, 7, storedStatus)))
            .when(collections).findOwned(1L, storedStatus);
        when(verifications.findRevealed(10, Status.SUCCESS)).thenReturn(List.of());
        artwork(null);

        mvc.perform(get("/api/collections")
                .param("exhibited", Boolean.toString(exhibited))
                .param("userId", "99")
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].isExhibited").value(exhibited));
        verify(collections).findOwned(1L, storedStatus);
        verifyNoMoreInteractions(collections);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsEmptyFilteredCollection(boolean exhibited) throws Exception {
        authenticate();
        when(collections.findOwned(1L, exhibited ? 'Y' : 'N')).thenReturn(List.of());
        mvc.perform(get("/api/collections")
                .param("exhibited", Boolean.toString(exhibited))
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void rejectsInvalidExhibitionFilter() throws Exception {
        authenticate();
        mvc.perform(get("/api/collections").param("exhibited", "invalid")
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(collections);
    }

    @Test
    void ignoresSpoofedUserIdAndReturnsEmptyArray() throws Exception {
        authenticate();
        when(collections.findOwned(1L, null)).thenReturn(List.of());
        mvc.perform(get("/api/collections").param("userId", "99")
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());
        verify(collections).findOwned(1L, null);
        verify(collections, never()).findOwned(99L, null);
        verifyNoInteractions(verifications, artworks, media);
    }

    @Test
    void rejectsRequestWithoutTokenBeforeDatabaseAccess() throws Exception {
        mvc.perform(get("/api/collections"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
        verifyNoInteractions(collections, verifications, artworks, media);
    }

    @Test
    void rejectsInvalidTokenBeforeDatabaseAccess() throws Exception {
        when(tokens.getUserId("invalid")).thenThrow(new IllegalArgumentException("Invalid token"));
        mvc.perform(get("/api/collections").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
        verifyNoInteractions(collections, verifications, artworks, media);
    }

    @ParameterizedTest
    @CsvSource({"7,0,0", "7,3,43", "7,7,100", "14,5,36", "30,20,67"})
    void calculatesProgressForSupportedDurations(int duration, int earned, int percent) throws Exception {
        authenticate();
        doReturn(List.of(challenge(10, duration, 'N'))).when(collections).findOwned(1L, null);
        when(verifications.findRevealed(10, Status.SUCCESS))
            .thenReturn(IntStream.range(0, earned).boxed().toList());
        artwork(null);

        mvc.perform(get("/api/collections").header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].isExhibited").value(false))
            .andExpect(jsonPath("$.data[0].progress.durationDays").value(duration))
            .andExpect(jsonPath("$.data[0].progress.successDays").value(earned))
            .andExpect(jsonPath("$.data[0].progress.progressPercent").value(percent))
            .andExpect(jsonPath("$.data[0].puzzle.totalPieceCount").value(duration));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {60L})
    void toleratesMissingMedia(Long mediaId) throws Exception {
        authenticate();
        doReturn(List.of(challenge(10, 7, 'N'))).when(collections).findOwned(1L, null);
        when(verifications.findRevealed(10, Status.SUCCESS)).thenReturn(List.of());
        artwork(mediaId);
        if (mediaId != null) {
            when(media.findById(mediaId)).thenReturn(Optional.empty());
        }
        mvc.perform(get("/api/collections").header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].artwork.artworkId").value(30))
            .andExpect(jsonPath("$.data[0].artwork.imageKey").doesNotExist());
        if (mediaId == null) {
            verifyNoInteractions(media);
        }
    }

    @Test
    void returnsNotFoundWhenArtworkIsMissingOrDeleted() throws Exception {
        authenticate();
        doReturn(List.of(challenge(10, 7, 'N'))).when(collections).findOwned(1L, null);
        when(artworks.findByArtworkIdAndDelYn(30L, "N")).thenReturn(Optional.empty());
        mvc.perform(get("/api/collections").header("Authorization", "Bearer owner-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void preservesRepositoryOrderAndKeepsEachChallengesPiecesSeparate() throws Exception {
        authenticate();
        doReturn(List.of(challenge(20, 7, 'N'), challenge(10, 7, 'Y'))).when(collections).findOwned(1L, null);
        when(verifications.findRevealed(20, Status.SUCCESS)).thenReturn(List.of(5));
        when(verifications.findRevealed(10, Status.SUCCESS)).thenReturn(List.of(0, 3));
        artwork(null);
        mvc.perform(get("/api/collections").header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].challengeId").value(20))
            .andExpect(jsonPath("$.data[0].challengeTitle").value("Practice guitar 20"))
            .andExpect(jsonPath("$.data[0].progress.successDays").value(1))
            .andExpect(jsonPath("$.data[0].puzzle.revealedPieceIndices").value(contains(5)))
            .andExpect(jsonPath("$.data[1].challengeId").value(10))
            .andExpect(jsonPath("$.data[1].challengeTitle").value("Practice guitar 10"))
            .andExpect(jsonPath("$.data[1].progress.successDays").value(2))
            .andExpect(jsonPath("$.data[1].puzzle.revealedPieceIndices").value(contains(0, 3)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void serviceRejectsInvalidUserBeforeQuery(Long userId) {
        assertThatThrownBy(() -> service.getCollections(userId, null))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED));
        verifyNoInteractions(collections, verifications, artworks, media);
    }

    private Challenge exhibitionChallenge(com.moamu.domain.challenge.enums.Status status) {
        Challenge challenge = Challenge.create(1L, 1L, 1L, 30L, "Practice guitar", "Practice", null,
            "Photo", 7, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7), "Photo", 3, 3);
        ReflectionTestUtils.setField(challenge, "challengeId", 10L);
        ReflectionTestUtils.setField(challenge, "status", status);
        when(collections.findOwnedForExhibition(1L, 10L)).thenReturn(Optional.of(challenge));
        return challenge;
    }

    @Test
    void exhibitsCompletedChallengeAndRepeatedRequestRemainsSuccessful() throws Exception {
        authenticate();
        Challenge challenge = exhibitionChallenge(com.moamu.domain.challenge.enums.Status.COMPLETED);
        assertThat(challenge.getIsExhibited()).isEqualTo('N');

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(patch("/api/collections/10/exhibition")
                    .header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.challengeId").value(10))
                .andExpect(jsonPath("$.data.isExhibited").value(true));
        }

        assertThat(challenge.getIsExhibited()).isEqualTo('Y');
        assertThat(challenge.getStatus()).isEqualTo(com.moamu.domain.challenge.enums.Status.COMPLETED);
        verifyNoInteractions(verifications, artworks, media);
    }

    @Test
    void collectionReadReflectsExhibitedState() throws Exception {
        authenticate();
        Challenge challenge = exhibitionChallenge(com.moamu.domain.challenge.enums.Status.COMPLETED);
        when(collections.findOwned(1L, null)).thenReturn(List.of(challenge));
        when(verifications.findRevealed(10, Status.SUCCESS)).thenReturn(List.of(0, 2));
        artwork(null);

        mvc.perform(patch("/api/collections/10/exhibition")
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/collections").header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].isExhibited").value(true))
            .andExpect(jsonPath("$.data[0].puzzle.revealedPieceIndices").value(contains(0, 2)));
    }

    @ParameterizedTest
    @EnumSource(value = com.moamu.domain.challenge.enums.Status.class,
        names = "COMPLETED", mode = EnumSource.Mode.EXCLUDE)
    void refusesEveryNonCompletedStatus(com.moamu.domain.challenge.enums.Status challengeStatus) throws Exception {
        authenticate();
        Challenge challenge = exhibitionChallenge(challengeStatus);
        mvc.perform(patch("/api/collections/10/exhibition")
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("CHALLENGE_NOT_COMPLETED"));
        assertThat(challenge.getIsExhibited()).isEqualTo('N');
        assertThat(challenge.getStatus()).isEqualTo(challengeStatus);
    }

    @Test
    void absentOwnedChallengeReturnsNotFoundAndCannotBeOverriddenByUserIdParameter() throws Exception {
        authenticate();
        when(collections.findOwnedForExhibition(1L, 10L)).thenReturn(Optional.empty());
        mvc.perform(patch("/api/collections/10/exhibition").param("userId", "99")
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        verify(collections).findOwnedForExhibition(1L, 10L);
        verifyNoMoreInteractions(collections);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "invalid"})
    void invalidChallengeIdIsRejectedBeforeQuery(String challengeId) throws Exception {
        authenticate();
        mvc.perform(patch("/api/collections/{challengeId}/exhibition", challengeId)
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        verifyNoInteractions(collections, verifications, artworks, media);
    }

    @Test
    void exhibitionRequiresAuthentication() throws Exception {
        mvc.perform(patch("/api/collections/10/exhibition"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
        when(tokens.getUserId("invalid")).thenThrow(new IllegalArgumentException("Invalid token"));
        mvc.perform(patch("/api/collections/10/exhibition")
                .header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
        verifyNoInteractions(collections, verifications, artworks, media);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void exhibitionServiceRejectsInvalidUserBeforeQuery(Long userId) {
        assertThatThrownBy(() -> service.exhibit(userId, 10L))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED));
        verifyNoInteractions(collections, verifications, artworks, media);
    }
}
