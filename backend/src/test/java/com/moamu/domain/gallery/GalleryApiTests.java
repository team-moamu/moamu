package com.moamu.domain.gallery;

import com.moamu.domain.artwork.entity.Artwork;
import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.gallery.controller.GalleryController;
import com.moamu.domain.gallery.repository.GalleryRepository;
import com.moamu.domain.gallery.service.GalleryService;
import com.moamu.domain.media.entity.Media;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.schedule.enums.Status;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.domain.verification.repository.VerificationSuccessRepository;
import com.moamu.domain.verification.service.ArtworkImageUrlService;
import com.moamu.global.config.SecurityConfig;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import com.moamu.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 실제 보안 필터·컨트롤러·서비스를 연결하고 DB 조회와 JWT 해석만 대역으로 격리한다.
@WebMvcTest(GalleryController.class)
@Import({SecurityConfig.class, GalleryService.class})
class GalleryApiTests {

    @Autowired MockMvc mvc;
    @Autowired GalleryService service;
    @MockitoBean GalleryRepository galleries;
    @MockitoBean VerificationSuccessRepository verifications;
    @MockitoBean ArtworkRepository artworks;
    @MockitoBean MediaRepository media;
    @MockitoBean ArtworkImageUrlService artworkImageUrls;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean UserRepository users;

    private void authenticate() {
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(tokens.getUserId("owner-token")).thenReturn(1L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
    }

    private Challenge challenge(long id, int duration) {
        Challenge challenge = mock(Challenge.class);
        when(challenge.getChallengeId()).thenReturn(id);
        when(challenge.getChallengeTitle()).thenReturn("Practice guitar " + id);
        when(challenge.getDurationDays()).thenReturn(duration);
        when(challenge.getEndDate()).thenReturn(LocalDate.of(2026, 11, 10));
        when(challenge.getArtworkId()).thenReturn(30L);
        when(challenge.getGridRows()).thenReturn(3);
        when(challenge.getGridCols()).thenReturn(3);
        return challenge;
    }

    private void artwork(Long mediaId) {
        Artwork artwork = mock(Artwork.class);
        when(artwork.getArtworkId()).thenReturn(30L);
        when(artwork.getArtworkTitle()).thenReturn("Painting");
        when(artwork.getMediaId()).thenReturn(mediaId);
        when(artworks.findByArtworkIdAndDelYn(30L, "N")).thenReturn(Optional.of(artwork));
    }

    @Test
    void returnsExhibitedGalleryWithLatestSortByDefault() throws Exception {
        authenticate();
        doReturn(List.of(challenge(10, 7)))
            .when(galleries).findExhibited(1L, "latest");
        when(verifications.findRevealed(10L, Status.SUCCESS)).thenReturn(List.of(0, 2, 6));
        artwork(60L);

        Media image = mock(Media.class);
        when(image.getImageKey()).thenReturn("artworks/test.png");
        when(media.findById(60L)).thenReturn(Optional.of(image));
        when(artworkImageUrls.resolve("artworks/test.png"))
            .thenReturn("https://example.com/artwork.png");

        mvc.perform(get("/api/galleries")
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].challengeId").value(10))
            .andExpect(jsonPath("$.data[0].challengeTitle").value("Practice guitar 10"))
            .andExpect(jsonPath("$.data[0].endDate").value("2026-11-10"))
            .andExpect(jsonPath("$.data[0].challengeStatus").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.data[0].isExhibited").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.data[0].progress.durationDays").value(7))
            .andExpect(jsonPath("$.data[0].progress.successDays").value(3))
            .andExpect(jsonPath("$.data[0].progress.progressPercent").value(43))
            .andExpect(jsonPath("$.data[0].artwork.artworkId").value(30))
            .andExpect(jsonPath("$.data[0].artwork.artworkTitle").value("Painting"))
            .andExpect(jsonPath("$.data[0].artwork.imageUrl")
                .value("https://example.com/artwork.png"))
            .andExpect(jsonPath("$.data[0].puzzle.gridRows").value(3))
            .andExpect(jsonPath("$.data[0].puzzle.gridCols").value(3))
            .andExpect(jsonPath("$.data[0].puzzle.revealedPieceIndices")
                .value(contains(0, 2, 6)))
            .andExpect(jsonPath("$.data[0].puzzle.earnedPieceCount").value(3))
            .andExpect(jsonPath("$.data[0].puzzle.totalPieceCount").value(7));

        verify(galleries).findExhibited(1L, "latest");
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest", "name", "successRate"})
    void supportsGallerySorts(String sort) throws Exception {
        authenticate();
        when(galleries.findExhibited(1L, sort)).thenReturn(List.of());

        mvc.perform(get("/api/galleries")
                .param("sort", sort)
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());

        verify(galleries).findExhibited(1L, sort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"oldest", "NAME", "invalid"})
    void rejectsInvalidSortBeforeRepositoryAccess(String sort) throws Exception {
        authenticate();

        mvc.perform(get("/api/galleries")
                .param("sort", sort)
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        verifyNoInteractions(galleries, verifications, artworks, media);
    }

    @Test
    void ignoresSpoofedUserIdAndUsesAuthenticatedOwner() throws Exception {
        authenticate();
        when(galleries.findExhibited(1L, "latest")).thenReturn(List.of());

        mvc.perform(get("/api/galleries")
                .param("userId", "99")
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());

        verify(galleries).findExhibited(1L, "latest");
        verify(galleries, never()).findExhibited(99L, "latest");
    }

    @Test
    void rejectsRequestWithoutTokenBeforeRepositoryAccess() throws Exception {
        mvc.perform(get("/api/galleries"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(galleries, verifications, artworks, media);
    }

    @ParameterizedTest
    @CsvSource({"7,0,0", "7,3,43", "7,7,100", "14,5,36", "30,20,67"})
    void calculatesProgress(int duration, int earned, int percent) throws Exception {
        authenticate();
        doReturn(List.of(challenge(10, duration)))
            .when(galleries).findExhibited(1L, "latest");
        when(verifications.findRevealed(10L, Status.SUCCESS))
            .thenReturn(IntStream.range(0, earned).boxed().toList());
        artwork(null);

        mvc.perform(get("/api/galleries")
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].progress.durationDays").value(duration))
            .andExpect(jsonPath("$.data[0].progress.successDays").value(earned))
            .andExpect(jsonPath("$.data[0].progress.progressPercent").value(percent))
            .andExpect(jsonPath("$.data[0].puzzle.totalPieceCount").value(duration));
    }

    @Test
    void returnsNotFoundWhenArtworkIsMissingOrDeleted() throws Exception {
        authenticate();
        doReturn(List.of(challenge(10, 7)))
            .when(galleries).findExhibited(1L, "latest");
        when(verifications.findRevealed(10L, Status.SUCCESS)).thenReturn(List.of());
        when(artworks.findByArtworkIdAndDelYn(30L, "N")).thenReturn(Optional.empty());

        mvc.perform(get("/api/galleries")
                .header("Authorization", "Bearer owner-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void serviceRejectsInvalidUserBeforeRepositoryAccess(Long userId) {
        assertThatThrownBy(() -> service.getGalleries(userId, "latest"))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED));

        verifyNoInteractions(galleries, verifications, artworks, media);
    }
}
