package com.moamu.domain.verification;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.domain.verification.dto.*;
import com.moamu.domain.verification.enums.*;
import com.moamu.domain.verification.exception.PhotoVerificationTechnicalException;
import com.moamu.domain.verification.service.*;
import com.moamu.global.exception.BusinessException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(BackendApplicationTests.ContainerConfiguration.class)
class VerificationSubmissionIntegrationTests {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ChallengeService challengeService;
    @Autowired private VerificationSubmissionService service;
    @MockitoBean private PhotoVerificationService ai;
    private long challengeId;
    private long scheduleId;
    private long mediaId;

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
                VALUES (20, 10, 'Read', 'Read daily', 'Show book', 'Book', 'ACTIVE')
                """);
        jdbc.update("INSERT INTO artwork (artwork_id) VALUES (30)");
        jdbc.update("INSERT INTO goal_template_artwork (goal_template_id, artwork_id) VALUES (20, 30)");
        challengeService.createChallenge(1L, new ChallengeCreateRequest(20L, 7,
                LocalDate.now(ZoneId.of("Asia/Seoul"))));
        challengeId = jdbc.queryForObject("SELECT challenge_id FROM challenge", Long.class);
        scheduleId = jdbc.queryForObject("SELECT schedule_id FROM schedule WHERE day_no = 1", Long.class);
        jdbc.update("INSERT INTO challenge_condition (challenge_id, code, description) VALUES (?, 'C1', 'Book')",
                challengeId);
        jdbc.update("""
                INSERT INTO media (image_key, file_hash, user_id, purpose)
                VALUES ('verification/book.png', ?, 1, 'VERIFICATION')
                """, "a".repeat(64));
        mediaId = jdbc.queryForObject("SELECT media_id FROM media", Long.class);
    }

    private VerificationSubmitRequest request() {
        return new VerificationSubmitRequest(challengeId, scheduleId, mediaId, UUID.randomUUID().toString());
    }

    private void respond(VerificationResult result, CheckStatus check) {
        when(ai.verify(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            PhotoVerificationRequest request = invocation.getArgument(0);
            assertThat(request.imageKey()).isEqualTo("verification/book.png");
            assertThat(request.conditions()).containsExactly(new PhotoVerificationRequest.Condition("C1", "Book"));
            return new PhotoVerificationService.Result(result, new PhotoVerificationResponse(
                    request.aiTaskId(), request.verificationId(), "SUCCEEDED",
                    List.of(new PhotoVerificationResponse.Check("C1", check, "Checked")), "test-model"));
        });
    }

    @Test
    void successPersistsResultAndDuplicateRequestDoesNotCallAiTwice() {
        respond(VerificationResult.SUCCESS, CheckStatus.MET);
        var request = request();
        var result = service.submit(1L, request);
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(service.submit(1L, request)).isEqualTo(result);
        assertThat(jdbc.queryForObject("SELECT status FROM schedule WHERE schedule_id = ?", String.class, scheduleId))
                .isEqualTo("SUCCESS");
        assertThat(jdbc.queryForObject("SELECT status FROM ai_task", String.class)).isEqualTo("SUCCEEDED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification_check", Integer.class)).isEqualTo(1);
        verify(ai, times(1)).verify(any());
    }

    @Test
    void failedJudgmentsAllowThreeAttemptsAndThenBlock() {
        respond(VerificationResult.FAILED, CheckStatus.NOT_MET);
        for (int i = 1; i <= 3; i++) assertThat(service.submit(1L, request()).attemptCount()).isEqualTo(i);
        assertThat(jdbc.queryForObject("SELECT status FROM schedule WHERE schedule_id = ?", String.class, scheduleId))
                .isEqualTo("FAILED");
        assertThatThrownBy(() -> service.submit(1L, request())).isInstanceOf(BusinessException.class);
        verify(ai, times(3)).verify(any());
    }

    @Test
    void technicalErrorCanRetrySameSubmissionWithoutAnotherAttempt() {
        var request = request();
        when(ai.verify(any())).thenThrow(new PhotoVerificationTechnicalException("Unavailable"));
        assertThatThrownBy(() -> service.submit(1L, request)).isInstanceOf(PhotoVerificationTechnicalException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM verification", String.class)).isEqualTo("TECH_ERROR");
        reset(ai);
        respond(VerificationResult.SUCCESS, CheckStatus.MET);
        assertThat(service.submit(1L, request).attemptCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isEqualTo(1);
    }

    @Test
    void uncertainJudgmentRequiresReviewAndPreventsNewSubmission() {
        respond(VerificationResult.REVIEW_REQUIRED, CheckStatus.UNCERTAIN);
        assertThat(service.submit(1L, request()).status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(jdbc.queryForObject("SELECT status FROM schedule WHERE schedule_id = ?", String.class, scheduleId))
                .isEqualTo("IN_REVIEW");
        assertThatThrownBy(() -> service.submit(1L, request())).isInstanceOf(BusinessException.class);
        verify(ai, times(1)).verify(any());
    }

    @Test
    void oldTechnicalErrorCannotOverwriteNewerJudgment() {
        var oldRequest = request();
        when(ai.verify(any())).thenThrow(new PhotoVerificationTechnicalException("Unavailable"));
        assertThatThrownBy(() -> service.submit(1L, oldRequest)).isInstanceOf(PhotoVerificationTechnicalException.class);
        reset(ai);
        respond(VerificationResult.FAILED, CheckStatus.NOT_MET);
        service.submit(1L, request());
        assertThatThrownBy(() -> service.submit(1L, oldRequest)).isInstanceOf(BusinessException.class);
        verify(ai, times(1)).verify(any());
    }

    @Test
    void missingAuthenticationOrWrongOwnershipNeverCallsAi() {
        assertThatThrownBy(() -> service.submit(null, request())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.submit(2L, request())).isInstanceOf(BusinessException.class);
        jdbc.update("INSERT INTO users (user_id, name) VALUES (2, 'Other user')");
        jdbc.update("UPDATE media SET user_id = 2 WHERE media_id = ?", mediaId);
        assertThatThrownBy(() -> service.submit(1L, request())).isInstanceOf(BusinessException.class);
        verifyNoInteractions(ai);
    }
}
