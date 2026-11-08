package com.moamu.domain.verification;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.verification.dto.PhotoVerificationRequest;
import com.moamu.domain.verification.dto.PhotoVerificationResponse;
import com.moamu.domain.verification.enums.CheckStatus;
import com.moamu.domain.verification.enums.VerificationResult;
import com.moamu.domain.verification.exception.PhotoVerificationTechnicalException;
import com.moamu.domain.verification.service.PhotoVerificationService;
import com.moamu.domain.verification.service.VerificationResultService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

// 실제 DB에서 결과 저장과 롤백을 검증합니다. 외부 AI/S3는 호출하지 않습니다.
@SpringBootTest
@ActiveProfiles("test")
@Import(BackendApplicationTests.ContainerConfiguration.class)
class VerificationResultTests {
    private static final long TASK_ID = 9007199254740993L;
    @Autowired JdbcTemplate jdbc;
    @Autowired VerificationResultService service;

    @BeforeEach
    void setUp() {
        clean();
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

    @AfterEach
    void clean() {
        for (String table : List.of("verification_check", "ai_task", "verification", "challenge_condition",
                "schedule", "challenge", "goal_template_artwork", "goal_template_condition", "goal_template",
                "artwork", "media", "goal_category", "users")) jdbc.update("DELETE FROM " + table);
    }

    private PhotoVerificationRequest request() {
        return new PhotoVerificationRequest(Long.toString(TASK_ID), "70", "verifications/photo.jpg", List.of(
                new PhotoVerificationRequest.Condition("C1", "명찰이 사진에 보여야 한다"),
                new PhotoVerificationRequest.Condition("C2", "완성된 결과물이 보여야 한다")));
    }

    private PhotoVerificationService.Result result(CheckStatus second) {
        var status = switch (second) {
            case MET -> VerificationResult.SUCCESS;
            case NOT_MET -> VerificationResult.FAILED;
            case UNCERTAIN -> VerificationResult.REVIEW_REQUIRED;
        };
        return new PhotoVerificationService.Result(status, new PhotoVerificationResponse(
                Long.toString(TASK_ID), "70", "SUCCEEDED", List.of(
                new PhotoVerificationResponse.Check("C1", CheckStatus.MET, "명찰 확인"),
                new PhotoVerificationResponse.Check("C2", second, "두 번째 조건 사유")), "test-model"));
    }

    @ParameterizedTest
    @EnumSource(CheckStatus.class)
    void savesAllFinalResultsAndConditionSnapshotIds(CheckStatus status) {
        var input = result(status);
        assertThat(service.save(request(), input)).isEqualTo(input);
        assertThat(jdbc.queryForObject("SELECT status FROM verification", String.class)).isEqualTo(input.status().name());
        assertThat(jdbc.queryForObject("SELECT model_version FROM verification", String.class)).isEqualTo("test-model");
        assertThat(jdbc.queryForMap("SELECT analyzed_at, upd_dt FROM verification").values()).doesNotContainNull();
        assertThat(jdbc.queryForObject("SELECT status FROM ai_task", String.class)).isEqualTo("SUCCEEDED");
        assertThat(jdbc.queryForMap("SELECT finished_at, upd_dt FROM ai_task").values()).doesNotContainNull();
        assertThat(jdbc.queryForList("SELECT challenge_condition_id FROM verification_check ORDER BY challenge_condition_id", Long.class))
                .containsExactly(61L, 62L);
        assertThat(jdbc.queryForObject("SELECT reason FROM verification_check WHERE challenge_condition_id=62", String.class))
                .isEqualTo("두 번째 조건 사유");
        assertThat(jdbc.queryForObject("SELECT reason FROM verification", String.class))
                .isEqualTo(status == CheckStatus.MET ? "모든 인증 조건을 충족했습니다." : "두 번째 조건 사유");
    }

    @ParameterizedTest
    @EnumSource(CheckStatus.class)
    void updatesScheduleWithoutConsumingAnotherAttempt(CheckStatus check) {
        service.save(request(), result(check));
        String expected = switch (check) {
            case MET -> "SUCCESS";
            case NOT_MET -> "OPEN";
            case UNCERTAIN -> "IN_REVIEW";
        };
        var saved = jdbc.queryForMap("SELECT status, attempt_count, version, decided_at, result_reason FROM schedule");
        assertThat(saved.get("status")).isEqualTo(expected);
        assertThat(saved.get("attempt_count")).isEqualTo(1);
        assertThat(saved.get("version")).isEqualTo(1);
        if (check == CheckStatus.MET) assertThat(saved.get("decided_at")).isNotNull();
        else assertThat(saved.get("decided_at")).isNull();
        if (check == CheckStatus.UNCERTAIN) assertThat(saved.get("result_reason")).isEqualTo("두 번째 조건 사유");
        else assertThat(saved.get("result_reason")).isNull();
    }

    @Test
    void thirdFailureFinalizesSchedule() {
        jdbc.update("UPDATE schedule SET attempt_count=3");
        service.save(request(), result(CheckStatus.NOT_MET));
        assertThat(jdbc.queryForObject("SELECT status FROM schedule", String.class)).isEqualTo("FAILED");
        assertThat(jdbc.queryForMap("SELECT decided_at, result_reason FROM schedule").values()).doesNotContainNull();
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isEqualTo(3);
    }

    @Test
    void finalizedScheduleCannotBeOverwritten() {
        jdbc.update("UPDATE schedule SET status='SUCCESS'");
        assertThatThrownBy(() -> service.save(request(), result(CheckStatus.NOT_MET)))
                .isInstanceOf(IllegalStateException.class);
        assertUnchanged();
        assertThat(jdbc.queryForObject("SELECT status FROM schedule", String.class)).isEqualTo("SUCCESS");
    }

    @Test
    void scheduleWriteFailureRollsBackAllResults() {
        jdbc.execute("ALTER TABLE schedule ADD CONSTRAINT test_schedule_failure CHECK(status <> 'SUCCESS')");
        try {
            assertThatThrownBy(() -> service.save(request(), result(CheckStatus.MET))).isInstanceOf(RuntimeException.class);
            assertUnchanged();
            assertThat(jdbc.queryForObject("SELECT status FROM schedule", String.class)).isEqualTo("PROGRESSING");
            assertThat(jdbc.queryForObject("SELECT version FROM schedule", Integer.class)).isZero();
        } finally { jdbc.execute("ALTER TABLE schedule DROP CHECK test_schedule_failure"); }
    }

    @Test
    void olderAttemptCannotUpdateScheduleForNewerSubmission() {
        jdbc.update("""
                INSERT INTO verification (verification_id, schedule_id, media_id, attempt_no,
                    idempotency_key, file_hash, submitted_at)
                VALUES (71, 50, 99, 2, 'newer-submission', ?, CURRENT_TIMESTAMP)
                """, "b".repeat(64));
        assertThatThrownBy(() -> service.save(request(), result(CheckStatus.MET)))
                .isInstanceOf(PhotoVerificationTechnicalException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM ai_task", String.class)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT status FROM verification WHERE verification_id=70", String.class)).isEqualTo("SUBMITTED");
        assertThat(jdbc.queryForObject("SELECT status FROM schedule", String.class)).isEqualTo("PROGRESSING");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification_check", Integer.class)).isZero();
    }

    @Test
    void duplicateResponseKeepsFirstResultAndTimestamps() {
        var first = service.save(request(), result(CheckStatus.MET));
        var before = jdbc.queryForMap("SELECT * FROM verification");
        var scheduleBefore = jdbc.queryForMap("SELECT * FROM schedule");
        assertThat(service.save(request(), result(CheckStatus.NOT_MET))).isEqualTo(first);
        assertThat(jdbc.queryForMap("SELECT * FROM verification")).isEqualTo(before);
        assertThat(jdbc.queryForMap("SELECT * FROM schedule")).isEqualTo(scheduleBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification_check", Integer.class)).isEqualTo(2);
    }

    @Test
    void concurrentDifferentResponsesCommitOneConsistentResult() throws Exception {
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> { start.await(); return service.save(request(), result(CheckStatus.MET)); });
            var second = pool.submit(() -> { start.await(); return service.save(request(), result(CheckStatus.NOT_MET)); });
            start.countDown();
            assertThat(first.get(15, java.util.concurrent.TimeUnit.SECONDS))
                    .isEqualTo(second.get(15, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification_check", Integer.class)).isEqualTo(2);
    }

    @Test
    void checkInsertFailureRollsBackTaskAndVerification() {
        // 두 번째 조건 INSERT를 실패시켜 첫 번째 조건까지 모두 롤백되는지 확인합니다.
        jdbc.execute("ALTER TABLE verification_check ADD CONSTRAINT test_check_failure CHECK(challenge_condition_id=61)");
        try {
            assertThatThrownBy(() -> service.save(request(), result(CheckStatus.MET)))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class)
                    .hasMessageContaining("test_check_failure");
            assertUnchanged();
        } finally { jdbc.execute("ALTER TABLE verification_check DROP CHECK test_check_failure"); }
        service.save(request(), result(CheckStatus.MET));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification_check", Integer.class)).isEqualTo(2);
    }

    @Test
    void invalidResponsesNeverWriteResults() {
        var response = result(CheckStatus.MET).response();
        for (var invalid : List.of(
                new PhotoVerificationResponse("999", "70", "SUCCEEDED", response.checks(), "model"),
                new PhotoVerificationResponse(Long.toString(TASK_ID), "999", "SUCCEEDED", response.checks(), "model"),
                new PhotoVerificationResponse(Long.toString(TASK_ID), "70", "ERROR", response.checks(), "model"),
                new PhotoVerificationResponse(Long.toString(TASK_ID), "70", "SUCCEEDED", List.of(), "model"),
                new PhotoVerificationResponse(Long.toString(TASK_ID), "70", "SUCCEEDED",
                        List.of(response.checks().getFirst(), response.checks().getFirst()), "model"),
                new PhotoVerificationResponse(Long.toString(TASK_ID), "70", "SUCCEEDED", response.checks(), "x".repeat(51)))) {
            assertThatThrownBy(() -> service.save(request(), new PhotoVerificationService.Result(VerificationResult.SUCCESS, invalid)))
                    .isInstanceOf(PhotoVerificationTechnicalException.class);
            assertUnchanged();
        }
    }

    @Test
    void rejectsMismatchedFinalResultAndChangedSnapshot() {
        assertThatThrownBy(() -> service.save(request(), new PhotoVerificationService.Result(
                VerificationResult.SUCCESS, result(CheckStatus.NOT_MET).response())))
                .isInstanceOf(PhotoVerificationTechnicalException.class);
        jdbc.update("UPDATE challenge_condition SET description='changed' WHERE challenge_condition_id=61");
        assertThatThrownBy(() -> service.save(request(), result(CheckStatus.MET)))
                .isInstanceOf(PhotoVerificationTechnicalException.class);
        assertUnchanged();
    }

    @Test
    void canceledTaskCannotBeCompleted() {
        jdbc.update("UPDATE ai_task SET status='CANCELED'");
        assertThatThrownBy(() -> service.save(request(), result(CheckStatus.MET)))
                .isInstanceOf(PhotoVerificationTechnicalException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM ai_task", String.class)).isEqualTo("CANCELED");
        assertThat(jdbc.queryForObject("SELECT status FROM verification", String.class)).isEqualTo("SUBMITTED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification_check", Integer.class)).isZero();
    }

    private void assertUnchanged() {
        assertThat(jdbc.queryForObject("SELECT status FROM ai_task", String.class)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT status FROM verification", String.class)).isEqualTo("SUBMITTED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification_check", Integer.class)).isZero();
        assertThat(jdbc.queryForMap("SELECT analyzed_at, model_version FROM verification").values()).containsOnlyNulls();
    }
}
