package com.moamu.domain.challenge;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
import com.moamu.domain.challenge.dto.ChallengeSettlementResponse;
import com.moamu.domain.challenge.enums.Status;
import com.moamu.domain.challenge.exception.ChallengeErrorCode;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import com.moamu.global.security.JwtTokenProvider;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "jwt.secret=bW9hbXUtdGVzdC1vbmx5LWtleS0zMi1ieXRlcy1sb25nIQ==")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(BackendApplicationTests.ContainerConfiguration.class)
class ChallengeSettlementIntegrationTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ChallengeService challengeService;
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokens;
    private long challengeId;

    @BeforeEach
    void setUp() {
        for (String table : List.of("verification_check", "ai_task", "verification", "schedule",
                "challenge_condition", "challenge", "goal_template_condition", "goal_template_artwork",
                "media", "goal_template", "artwork", "goal_category", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("INSERT INTO users (user_id, name) VALUES (1, 'Test')");
        jdbc.update("INSERT INTO goal_category (cate_id, cate_code, cate_name) VALUES (10, 'TEST', 'Test')");
        jdbc.update("""
            INSERT INTO goal_template (goal_template_id, cate_id, goal_name, description,
                photo_guide, verification_scope, status)
            VALUES (20, 10, 'Read', 'Read daily', 'Book', 'Book', 'ACTIVE')
            """);
        jdbc.update("INSERT INTO artwork (artwork_id) VALUES (30)");
        jdbc.update("INSERT INTO goal_template_artwork (goal_template_id, artwork_id) VALUES (20, 30)");
        challengeService.createChallenge(1L, new ChallengeCreateRequest(20L, 7,
            LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(7)));
        challengeId = jdbc.queryForObject("SELECT challenge_id FROM challenge", Long.class);
        jdbc.update("UPDATE challenge SET status='IN_PROGRESS'");
        jdbc.update("UPDATE schedule SET status='SUCCESS'");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 7})
    void completesWithAllPartialOrZeroPiecesWithoutChangingSchedulesOrExhibition(int successCount) {
        jdbc.update("UPDATE schedule SET status='FAILED' WHERE day_no>?", successCount);
        List<java.util.Map<String, Object>> schedulesBefore = jdbc.queryForList("SELECT * FROM schedule ORDER BY day_no");

        ChallengeSettlementResponse response = challengeService.settle(1L, challengeId);

        assertThat(response.status()).isEqualTo(Status.COMPLETED);
        assertThat(response.successScheduleCount()).isEqualTo(successCount);
        assertThat(response.failedScheduleCount()).isEqualTo(7 - successCount);
        assertThat(response.completionAvailable()).isTrue();
        assertThat(challengeService.settle(1L, challengeId)).isEqualTo(response);
        assertThat(jdbc.queryForList("SELECT * FROM schedule ORDER BY day_no")).isEqualTo(schedulesBefore);
        assertThat(jdbc.queryForObject("SELECT is_exhibited FROM challenge", String.class)).isEqualTo("N");
        assertThat(jdbc.queryForObject("SELECT status FROM challenge", String.class)).isEqualTo("COMPLETED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"UPCOMING", "OPEN", "PROGRESSING", "IN_REVIEW", "PENDING_SETTLEMENT"})
    void leavesUnconfirmedScheduleInSettlement(String status) {
        jdbc.update("UPDATE schedule SET status=? WHERE day_no=7", status);
        ChallengeSettlementResponse response = challengeService.settle(1L, challengeId);
        assertThat(response.status()).isEqualTo(Status.SETTLING);
        assertThat(response.pendingScheduleCount()).isEqualTo(1);
        assertThat(response.completionAvailable()).isFalse();

        // 회차 담당 로직이 결과를 확정한 후 재호출하면 도전 완료
        jdbc.update("UPDATE schedule SET status='FAILED' WHERE day_no=7");
        assertThat(challengeService.settle(1L, challengeId).status()).isEqualTo(Status.COMPLETED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUBMITTED", "ANALYZING", "REVIEW_REQUIRED", "TECH_ERROR"})
    void waitsForLatestUnresolvedVerificationEvenIfScheduleWasFinalized(String status) {
        insertVerification(1, status);
        ChallengeSettlementResponse response = challengeService.settle(1L, challengeId);
        assertThat(response.status()).isEqualTo(Status.SETTLING);
        assertThat(response.pendingVerificationCount()).isEqualTo(1);
        assertThat(response.completionAvailable()).isFalse();
    }

    @Test
    void oldTechnicalErrorDoesNotBlockNewerFinalResult() {
        insertVerification(1, "TECH_ERROR");
        insertVerification(2, "SUCCESS");
        assertThat(challengeService.settle(1L, challengeId).status()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void rejectsOnEndDateAndLeavesStateUnchanged() {
        jdbc.update("UPDATE challenge SET start_date=DATE_ADD(start_date, INTERVAL 1 DAY), end_date=DATE_ADD(end_date, INTERVAL 1 DAY)");
        assertThatThrownBy(() -> challengeService.settle(1L, challengeId))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ChallengeErrorCode.CHALLENGE_NOT_ENDED));
        assertThat(jdbc.queryForObject("SELECT status FROM challenge", String.class)).isEqualTo("IN_PROGRESS");
    }

    @ParameterizedTest
    @ValueSource(strings = {"READY", "STOPPED"})
    void rejectsNonRunningChallenges(String status) {
        jdbc.update("UPDATE challenge SET status=?", status);
        assertThatThrownBy(() -> challengeService.settle(1L, challengeId))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ChallengeErrorCode.CHALLENGE_SETTLEMENT_NOT_ALLOWED));
    }

    @Test
    void rejectsMissingOrWrongOwnerOrDeletedChallenge() {
        assertThatThrownBy(() -> challengeService.settle(null, challengeId)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> challengeService.settle(1L, 0L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> challengeService.settle(2L, challengeId))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        jdbc.update("UPDATE challenge SET del_yn='Y'");
        assertThatThrownBy(() -> challengeService.settle(1L, challengeId)).isInstanceOf(BusinessException.class);
    }

    @Test
    void missingScheduleCannotCompleteChallenge() {
        jdbc.update("DELETE FROM schedule WHERE day_no=7");
        assertThatThrownBy(() -> challengeService.settle(1L, challengeId))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ChallengeErrorCode.INVALID_CHALLENGE_SCHEDULES));
        assertThat(jdbc.queryForObject("SELECT status FROM challenge", String.class)).isEqualTo("IN_PROGRESS");
    }

    @Test
    void httpEndpointRequiresJwtAndUsesItsOwner() throws Exception {
        String path = "/api/challenges/" + challengeId + "/settlement";
        mockMvc.perform(post(path)).andExpect(status().isUnauthorized());
        jdbc.update("INSERT INTO users (user_id, name) VALUES (2, 'Other')");
        mockMvc.perform(post(path).header("Authorization", "Bearer " + tokens.createAccessToken(2L)))
            .andExpect(status().isNotFound());
        mockMvc.perform(post(path).header("Authorization", "Bearer " + tokens.createAccessToken(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.completionAvailable").value(true));
    }

    @Test
    void concurrentRequestsReturnSameCompletion() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<ChallengeSettlementResponse> request = () -> challengeService.settle(1L, challengeId);
            var responses = executor.invokeAll(List.of(request, request));
            assertThat(responses.get(0).get()).isEqualTo(responses.get(1).get());
            assertThat(responses.get(0).get().status()).isEqualTo(Status.COMPLETED);
        }
    }

    private void insertVerification(int attempt, String status) {
        if (attempt == 1) {
            jdbc.update("INSERT INTO media (media_id, image_key, file_hash, user_id, purpose) VALUES (60, 'verification/test.png', ?, 1, 'VERIFICATION')", "a".repeat(64));
        }
        Long scheduleId = jdbc.queryForObject("SELECT schedule_id FROM schedule WHERE day_no=7", Long.class);
        jdbc.update("""
            INSERT INTO verification (schedule_id, media_id, attempt_no, idempotency_key, file_hash, status, submitted_at)
            VALUES (?, 60, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """, scheduleId, attempt, "settlement-" + attempt, "a".repeat(64), status);
    }
}
