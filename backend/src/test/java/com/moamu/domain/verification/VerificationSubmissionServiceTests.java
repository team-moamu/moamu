package com.moamu.domain.verification;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.verification.client.PhotoVerificationClient;
import com.moamu.domain.verification.dto.PhotoVerificationRequest;
import com.moamu.domain.verification.dto.PhotoVerificationResponse;
import com.moamu.domain.verification.enums.CheckStatus;
import com.moamu.domain.verification.enums.VerificationResult;
import com.moamu.domain.verification.service.VerificationSubmissionService;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 실제 MySQL의 연결 관계로 요청을 구성
@SpringBootTest
@ActiveProfiles("test")
@Import(BackendApplicationTests.ContainerConfiguration.class)
@Transactional
class VerificationSubmissionServiceTests {
    private static final long TASK_ID = 9007199254740993L;
    @Autowired JdbcTemplate jdbc;
    @Autowired VerificationSubmissionService service;
    @MockitoBean PhotoVerificationClient client;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO users (user_id, name) VALUES (1, 'Owner'), (2, 'Other')");
        jdbc.update("INSERT INTO goal_category (cate_id, cate_code, cate_name) VALUES (10, 'TEST', 'Test')");
        jdbc.update("""
                INSERT INTO goal_template (goal_template_id, cate_id, goal_name, photo_guide, verification_scope)
                VALUES (20, 10, 'Test', 'Guide', 'Visible result')
                """);
        jdbc.update("INSERT INTO artwork (artwork_id) VALUES (30)");
        jdbc.update("""
                INSERT INTO challenge (challenge_id, user_id, cate_id, goal_template_id, artwork_id,
                    challenge_title, goal_text, duration_days, start_date, end_date, verification_scope, grid_rows, grid_cols)
                VALUES (40, 1, 10, 20, 30, 'Test', 'Daily goal', 7, '2026-11-01', '2026-11-07', 'Visible result', 3, 3)
                """);
        jdbc.update("""
                INSERT INTO schedule (schedule_id, challenge_id, day_no, schedule_date, piece_index, status, attempt_count)
                VALUES (50, 40, 1, '2026-11-01', 0, 'PROGRESSING', 1)
                """);
        jdbc.update("""
                INSERT INTO challenge_condition (challenge_condition_id, challenge_id, code, description)
                VALUES (62, 40, 'C2', '완성된 결과물이 보여야 한다'), (61, 40, 'C1', '명찰이 사진에 보여야 한다')
                """);
        jdbc.update("""
                INSERT INTO media (media_id, image_key, file_hash, user_id, purpose)
                VALUES (60, 'verifications/photo.jpg', ?, 1, 'VERIFICATION'),
                       (99, 'verifications/unrelated.jpg', ?, 1, 'VERIFICATION')
                """, "a".repeat(64), "b".repeat(64));
        jdbc.update("""
                INSERT INTO verification (verification_id, schedule_id, media_id, attempt_no,
                    idempotency_key, file_hash, submitted_at)
                VALUES (70, 50, 60, 1, 'existing-submission', ?, CURRENT_TIMESTAMP)
                """, "a".repeat(64));
        jdbc.update("INSERT INTO ai_task (ai_task_id, task_type, verification_id) VALUES (?, 'VERIFY', 70)", TASK_ID);
    }

    @Test
    void sendsLinkedDatabaseValuesToExistingAiService() {
        var expected = new PhotoVerificationRequest(Long.toString(TASK_ID), "70", "verifications/photo.jpg", List.of(
                new PhotoVerificationRequest.Condition("C1", "명찰이 사진에 보여야 한다"),
                new PhotoVerificationRequest.Condition("C2", "완성된 결과물이 보여야 한다")));
        var response = new PhotoVerificationResponse(Long.toString(TASK_ID), "70", "SUCCEEDED", List.of(
                new PhotoVerificationResponse.Check("C1", CheckStatus.MET, "명찰 확인"),
                new PhotoVerificationResponse.Check("C2", CheckStatus.MET, "결과물 확인")), "test-model");
        when(client.verify(expected)).thenReturn(response);

        var result = service.verify(1L, TASK_ID);

        verify(client).verify(expected);
        assertThat(result.status()).isEqualTo(VerificationResult.SUCCESS);
        assertThat(result.response()).isEqualTo(response);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM ai_task", String.class)).isEqualTo("SUCCEEDED");
        assertThat(jdbc.queryForObject("SELECT status FROM verification", String.class)).isEqualTo("SUCCESS");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification_check", Integer.class)).isEqualTo(2);
    }

    @Test
    void doesNotSendOtherUsersTaskOrPhoto() {
        assertError(2L, TASK_ID, CommonErrorCode.RESOURCE_NOT_FOUND);
        jdbc.update("UPDATE media SET user_id = 2 WHERE media_id = 60");
        assertError(1L, TASK_ID, CommonErrorCode.RESOURCE_NOT_FOUND);
        verifyNoInteractions(client);
    }

    @Test
    void rejectsMissingTaskAndWrongTaskType() {
        assertError(1L, 999L, CommonErrorCode.RESOURCE_NOT_FOUND);
        jdbc.update("UPDATE ai_task SET task_type = 'GENERATE_ARTWORK'");
        assertError(1L, TASK_ID, CommonErrorCode.RESOURCE_NOT_FOUND);
        verifyNoInteractions(client);
    }

    @Test
    void rejectsEmptyConditionsBeforeCallingAi() {
        jdbc.update("DELETE FROM challenge_condition");
        assertThatThrownBy(() -> service.verify(1L, TASK_ID))
                .isInstanceOf(com.moamu.domain.verification.exception.PhotoVerificationTechnicalException.class);
        verifyNoInteractions(client);
    }

    @Test
    void validatesUserAndTaskId() {
        assertError(null, TASK_ID, CommonErrorCode.UNAUTHORIZED);
        assertError(0L, TASK_ID, CommonErrorCode.UNAUTHORIZED);
        assertError(1L, null, CommonErrorCode.INVALID_REQUEST);
        assertError(1L, 0L, CommonErrorCode.INVALID_REQUEST);
        verifyNoInteractions(client);
    }

    @Test
    void recordsTechnicalFailureWithoutCreatingAnotherAttempt() {
        var failure = new IllegalStateException("AI unavailable");
        when(client.verify(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.verify(1L, TASK_ID)).isSameAs(failure);
        assertThat(jdbc.queryForObject("SELECT status FROM ai_task", String.class)).isEqualTo("FAILED_FINAL");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isEqualTo(1);
    }

    private void assertError(Long userId, Long taskId, CommonErrorCode expected) {
        assertThatThrownBy(() -> service.verify(userId, taskId)).isInstanceOfSatisfying(
                BusinessException.class, error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }
}
