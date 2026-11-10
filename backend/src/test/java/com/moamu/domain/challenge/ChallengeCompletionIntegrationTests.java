package com.moamu.domain.challenge;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.challenge.dto.ChallengeCompletionResponse;
import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
import com.moamu.domain.challenge.enums.Status;
import com.moamu.domain.challenge.exception.ChallengeErrorCode;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.domain.verification.service.ArtworkImageUrlService;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import com.moamu.global.security.JwtTokenProvider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "jwt.secret=bW9hbXUtdGVzdC1vbmx5LWtleS0zMi1ieXRlcy1sb25nIQ==")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(BackendApplicationTests.ContainerConfiguration.class)
class ChallengeCompletionIntegrationTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ChallengeService challengeService;
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokens;
    @MockitoBean private ArtworkImageUrlService artworkImageUrlService;
    private long challengeId;

    @BeforeEach
    void setUp() {
        for (String table : List.of("verification_check", "ai_task", "verification", "schedule",
                "challenge_condition", "challenge", "goal_template_condition", "goal_template_artwork",
                "media", "goal_template", "artwork", "goal_category", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("INSERT INTO users (user_id, name) VALUES (1, 'Owner'), (2, 'Other')");
        jdbc.update("INSERT INTO goal_category (cate_id, cate_code, cate_name) VALUES (10, 'TEST', 'Test')");
        jdbc.update("""
            INSERT INTO goal_template (goal_template_id, cate_id, goal_name, description,
                photo_guide, verification_scope, status)
            VALUES (20, 10, 'Read', 'Read daily', 'Book', 'Book', 'ACTIVE')
            """);
        jdbc.update("INSERT INTO media (media_id, image_key, purpose) VALUES (60, 'artworks/test.png', 'VERIFICATION')");
        jdbc.update("INSERT INTO artwork (artwork_id, artwork_title, media_id) VALUES (30, 'Painting', 60)");
        jdbc.update("INSERT INTO goal_template_artwork (goal_template_id, artwork_id) VALUES (20, 30)");
        when(artworkImageUrlService.resolve("artworks/test.png")).thenReturn("https://example.com/signed-artwork");
        createCompletedChallenge(7);
    }

    private void createCompletedChallenge(int duration) {
        jdbc.update("DELETE FROM schedule");
        jdbc.update("DELETE FROM challenge");
        challengeService.createChallenge(1L, new ChallengeCreateRequest(20L, duration,
            LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(duration)));
        challengeId = jdbc.queryForObject("SELECT challenge_id FROM challenge", Long.class);
        jdbc.update("UPDATE challenge SET status='IN_PROGRESS'");
        jdbc.update("UPDATE schedule SET status='SUCCESS'");
        challengeService.settle(1L, challengeId);
    }

    @ParameterizedTest
    @CsvSource({"7,0", "7,3", "7,7", "14,0", "14,8", "14,14", "30,0", "30,15", "30,30"})
    void returnsActualPeriodAndAllPartialOrZeroPiecesWithoutMutation(int duration, int earned) {
        createCompletedChallenge(duration);
        jdbc.update("UPDATE schedule SET status='FAILED' WHERE day_no>?", earned);
        List<java.util.Map<String, Object>> challengeBefore = jdbc.queryForList("SELECT * FROM challenge");
        List<java.util.Map<String, Object>> schedulesBefore = jdbc.queryForList("SELECT * FROM schedule ORDER BY day_no");

        ChallengeCompletionResponse response = challengeService.getCompletion(1L, challengeId);

        assertThat(response.status()).isEqualTo(Status.COMPLETED);
        assertThat(response.challengeTitle()).isEqualTo("Read");
        assertThat(response.durationDays()).isEqualTo(duration);
        assertThat(response.startDate().plusDays(duration - 1)).isEqualTo(response.endDate());
        assertThat(response.totalPieceCount()).isEqualTo(duration);
        assertThat(response.earnedPieceCount()).isEqualTo(earned);
        assertThat(response.missingPieceCount()).isEqualTo(duration - earned);
        assertThat(response.allPiecesEarned()).isEqualTo(duration == earned);
        assertThat(response.revealedPieceIndices()).containsExactlyElementsOf(
            java.util.stream.IntStream.range(0, earned).boxed().toList());
        assertThat(response.artwork().artworkId()).isEqualTo(30L);
        assertThat(response.artwork().title()).isEqualTo("Painting");
        assertThat(response.artwork().imageUrl()).isEqualTo("https://example.com/signed-artwork");
        int cols = (int) Math.ceil(Math.sqrt(duration));
        assertThat(response.gridCols()).isEqualTo(cols);
        assertThat(response.gridRows()).isEqualTo((int) Math.ceil((double) duration / cols));
        assertThat(challengeService.getCompletion(1L, challengeId)).isEqualTo(response);
        assertThat(jdbc.queryForList("SELECT * FROM challenge")).isEqualTo(challengeBefore);
        assertThat(jdbc.queryForList("SELECT * FROM schedule ORDER BY day_no")).isEqualTo(schedulesBefore);
    }

    @Test
    void returnsRealAssignedPositionsRatherThanFirstEarnedCells() {
        jdbc.update("UPDATE schedule SET status='FAILED' WHERE piece_index NOT IN (1,4,6)");
        ChallengeCompletionResponse response = challengeService.getCompletion(1L, challengeId);
        assertThat(response.revealedPieceIndices()).containsExactly(1, 4, 6);
        assertThat(response.missingPieceCount()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"READY", "IN_PROGRESS", "SETTLING", "STOPPED"})
    void rejectsNonCompletedChallengeWithoutChangingStatus(String status) {
        jdbc.update("UPDATE challenge SET status=?", status);
        assertNotCompleted();
        assertThat(jdbc.queryForObject("SELECT status FROM challenge", String.class)).isEqualTo(status);
        verify(artworkImageUrlService, never()).resolve(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UPCOMING", "OPEN", "PROGRESSING", "IN_REVIEW", "PENDING_SETTLEMENT"})
    void refusesInconsistentCompletedChallengeWithPendingSchedule(String status) {
        jdbc.update("UPDATE schedule SET status=? WHERE day_no=7", status);
        assertNotCompleted();
    }

    @Test
    void allowsCompletedChallengeOnEndDate() {
        jdbc.update("UPDATE challenge SET start_date=DATE_ADD(start_date, INTERVAL 1 DAY), end_date=DATE_ADD(end_date, INTERVAL 1 DAY)");
        jdbc.update("UPDATE schedule SET schedule_date=DATE_ADD(schedule_date, INTERVAL 1 DAY)");
        assertThat(challengeService.getCompletion(1L, challengeId).status()).isEqualTo(Status.COMPLETED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUBMITTED", "ANALYZING", "REVIEW_REQUIRED", "TECH_ERROR"})
    void refusesLatestUnresolvedVerification(String status) {
        Long scheduleId = jdbc.queryForObject("SELECT schedule_id FROM schedule WHERE day_no=7", Long.class);
        jdbc.update("""
            INSERT INTO verification (schedule_id, media_id, attempt_no, idempotency_key, file_hash, status, submitted_at)
            VALUES (?, 60, 1, 'completion-test', ?, ?, CURRENT_TIMESTAMP)
            """, scheduleId, "a".repeat(64), status);
        assertNotCompleted();
    }

    @Test
    void rejectsMissingScheduleOrOutOfRangePiece() {
        jdbc.update("UPDATE schedule SET piece_index=100 WHERE day_no=7");
        assertThatThrownBy(() -> challengeService.getCompletion(1L, challengeId))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ChallengeErrorCode.INVALID_CHALLENGE_SCHEDULES));
        jdbc.update("DELETE FROM schedule WHERE day_no=7");
        assertThatThrownBy(() -> challengeService.getCompletion(1L, challengeId)).isInstanceOf(BusinessException.class);
    }

    @Test
    void permitsMissingMediaWithoutInventingAnImage() {
        jdbc.update("UPDATE artwork SET media_id=NULL");
        assertThat(challengeService.getCompletion(1L, challengeId).artwork().imageUrl()).isNull();
        verify(artworkImageUrlService).resolve(null);
    }

    @Test
    void usesChallengeArtworkEvenIfTemplateMappingChanges() {
        jdbc.update("DELETE FROM goal_template_artwork");
        assertThat(challengeService.getCompletion(1L, challengeId).artwork().artworkId()).isEqualTo(30);
        verify(artworkImageUrlService).resolve("artworks/test.png");
    }

    @Test
    void endpointChecksJwtOwnershipAndResponse() throws Exception {
        String path = "/api/challenges/" + challengeId + "/completion";
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path).header("Authorization", "Bearer " + tokens.createAccessToken(2L)))
            .andExpect(status().isNotFound());
        mockMvc.perform(get(path).header("Authorization", "Bearer " + tokens.createAccessToken(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.totalPieceCount").value(7))
            .andExpect(jsonPath("$.data.artwork.imageUrl").value("https://example.com/signed-artwork"));
        jdbc.update("UPDATE challenge SET del_yn='Y'");
        mockMvc.perform(get(path).header("Authorization", "Bearer " + tokens.createAccessToken(1L)))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidInputAndMissingChallenge() {
        assertThatThrownBy(() -> challengeService.getCompletion(null, challengeId)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> challengeService.getCompletion(1L, 0L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> challengeService.getCompletion(1L, Long.MAX_VALUE))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    private void assertNotCompleted() {
        assertThatThrownBy(() -> challengeService.getCompletion(1L, challengeId))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ChallengeErrorCode.CHALLENGE_NOT_COMPLETED));
    }
}
