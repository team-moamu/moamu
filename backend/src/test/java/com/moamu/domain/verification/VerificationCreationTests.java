package com.moamu.domain.verification;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.verification.client.PhotoVerificationClient;
import com.moamu.domain.verification.service.VerificationSubmissionService;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

// 격리된 MySQL에서 접수 저장, 멱등성, 낙관적 락과 롤백을 검증합니다. AI는 호출하지 않습니다.
@SpringBootTest
@ActiveProfiles("test")
@Import(BackendApplicationTests.ContainerConfiguration.class)
class VerificationCreationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired VerificationSubmissionService service;
    @Autowired com.moamu.domain.verification.repository.PhotoVerificationRequestRepository requestRepository;
    @MockitoBean PhotoVerificationClient client;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.moamu.domain.verification.repository.VerificationRepository verificationRepository;

    @BeforeEach
    void setUp() {
        reset(verificationRepository);
        clean();
        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
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
                VALUES (40, 1, 10, 20, 30, 'Test', 'Daily goal', 7, ?, ?, 'Visible result', 3, 3)
                """, today, today.plusDays(6));
        jdbc.update("""
                INSERT INTO schedule (schedule_id, challenge_id, day_no, schedule_date, piece_index)
                VALUES (50, 40, 1, ?, 0)
                """, today);
        jdbc.update("""
                INSERT INTO challenge_condition (challenge_condition_id, challenge_id, code, description)
                VALUES (62, 40, 'C2', '완성된 결과물이 보여야 한다'), (61, 40, 'C1', '명찰이 사진에 보여야 한다')
                """);
        jdbc.update("""
                INSERT INTO media (media_id, image_key, file_hash, user_id, purpose)
                VALUES (60, 'verifications/photo.jpg', ?, 1, 'VERIFICATION'),
                       (99, 'verifications/unrelated.jpg', ?, 1, 'VERIFICATION')
                """, "a".repeat(64), "b".repeat(64));
    }

    @org.junit.jupiter.api.AfterEach
    void clean() {
        for (String table : List.of("verification_check", "ai_task", "verification", "challenge_condition",
                "schedule", "challenge", "goal_template_artwork", "goal_template_condition", "goal_template",
                "artwork", "media", "goal_category", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    @Test
    void createsSubmittedVerificationAndReservesAttemptWithoutCallingAi() {
        var result = service.createVerification(1L, 50L, 60L, "first");
        assertThat(result.verificationId()).isPositive();
        assertThat(result.mediaId()).isEqualTo(60L);
        assertThat(result.attemptNo()).isEqualTo(1);
        assertThat(result.status()).isEqualTo("SUBMITTED");
        var saved = jdbc.queryForMap("SELECT * FROM verification");
        assertThat(saved.get("status")).isEqualTo("SUBMITTED");
        assertThat(verificationRepository.findById(result.verificationId()).orElseThrow().getStatus())
                .isEqualTo(com.moamu.domain.verification.enums.VerificationStatus.SUBMITTED);
        assertThat(saved.get("idempotency_key")).isEqualTo("first");
        assertThat(saved.get("file_hash")).isEqualTo("a".repeat(64));
        assertThat(saved.get("submitted_at")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM schedule", String.class)).isEqualTo("PROGRESSING");
        assertThat(result.aiTaskId()).isPositive();
        var task = jdbc.queryForMap("SELECT * FROM ai_task WHERE ai_task_id=?", result.aiTaskId());
        assertThat(((Number) task.get("verification_id")).longValue()).isEqualTo(result.verificationId());
        assertThat(task.get("task_type")).isEqualTo("VERIFY");
        assertThat(task.get("status")).isEqualTo("PENDING");
        assertThat(task.get("task_version")).isEqualTo(1);
        assertThat(task.get("retry_count")).isEqualTo(0);
        assertThat(task.get("frst_reg_dt")).isNotNull();
        for (String field : List.of("artwork_id", "next_retry_at", "locked_by", "locked_at",
                "last_error", "started_at", "finished_at")) {
            assertThat(task.get(field)).as(field).isNull();
        }
        // 새로 저장한 정보로 기존 AI 요청 조회 메서드가 올바른 요청을 구성하는지 확인합니다.
        var request = requestRepository.findByAiTaskIdAndUserId(result.aiTaskId(), 1L).orElseThrow();
        assertThat(request.aiTaskId()).isEqualTo(result.aiTaskId().toString());
        assertThat(request.verificationId()).isEqualTo(result.verificationId().toString());
        assertThat(request.imageKey()).isEqualTo("verifications/photo.jpg");
        assertThat(request.conditions()).extracting(condition -> condition.code()).containsExactly("C1", "C2");
        assertThat(requestRepository.findByAiTaskIdAndUserId(result.aiTaskId(), 2L)).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void sameRequestReturnsExistingIdAndDifferentPhotoCannotReuseKey() {
        var first = service.createVerification(1L, 50L, 60L, "same");
        assertThat(service.createVerification(1L, 50L, 60L, "same")).isEqualTo(first);
        assertThatThrownBy(() -> service.createVerification(1L, 50L, 99L, "same"))
                .isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isEqualTo(1);
    }

    @Test
    void simultaneousReplayCreatesOnlyOneRow() throws Exception {
        forceConcurrentVersionReads();
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> { start.await(); return service.createVerification(1L, 50L, 60L, "same"); });
            var second = pool.submit(() -> { start.await(); return service.createVerification(1L, 50L, 60L, "same"); });
            start.countDown();
            assertThat(first.get(15, java.util.concurrent.TimeUnit.SECONDS))
                    .isEqualTo(second.get(15, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isEqualTo(1);
    }

    // 두 트랜잭션이 같은 회차 버전을 읽은 뒤 진행하도록 해서 충돌을 확실히 재현합니다.
    private void forceConcurrentVersionReads() {
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(invocation -> {
            // 프록시의 추상 메서드를 직접 호출하지 않고 실제 DB에서 순번을 조회합니다.
            int lastAttempt = jdbc.queryForObject(
                    "SELECT COALESCE(MAX(attempt_no), 0) FROM verification WHERE schedule_id=50", Integer.class);
            if (calls.incrementAndGet() <= 2) {
                barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
            }
            return lastAttempt;
        }).when(verificationRepository).findLastAttemptNo(50L);
    }

    @Test
    void concurrentDifferentSubmissionsAcceptOnlyOne() throws Exception {
        forceConcurrentVersionReads();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> submitOrReject(60L, "first-concurrent"));
            var second = pool.submit(() -> submitOrReject(99L, "second-concurrent"));
            assertThat(List.of(first.get(15, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(15, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT version FROM schedule", Integer.class)).isEqualTo(1);
    }

    private boolean submitOrReject(Long mediaId, String key) {
        try {
            service.createVerification(1L, 50L, mediaId, key);
            return true;
        } catch (BusinessException exception) {
            assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST);
            return false;
        }
    }

    @Test
    void insertFailureRollsBackScheduleVersionAndAttempt() {
        jdbc.execute("ALTER TABLE verification ADD CONSTRAINT test_insert_failure CHECK(attempt_no=0)");
        try {
            assertThatThrownBy(() -> service.createVerification(1L, 50L, 60L, "insert-failure"))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("test_insert_failure");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT version FROM schedule", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT status FROM schedule", String.class)).isEqualTo("UPCOMING");
        } finally { jdbc.execute("ALTER TABLE verification DROP CHECK test_insert_failure"); }
    }

    @Test
    void optimisticConflictRetriesAndCreatesOnlyOneVerification() {
        // 첫 시도만 충돌시키고, 다음 트랜잭션에서 정상 접수되는지 확인합니다.
        doThrow(new OptimisticLockingFailureException("test conflict"))
                .doReturn(0).when(verificationRepository).findLastAttemptNo(50L);
        var result = service.createVerification(1L, 50L, 60L, "retry-once");
        assertThat(result.attemptNo()).isEqualTo(1);
        verify(verificationRepository, times(2)).findByIdempotencyKey("retry-once");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT version FROM schedule", Integer.class)).isEqualTo(1);
    }

    @Test
    void repeatedOptimisticConflictsStopAfterThreeAttempts() {
        doThrow(new OptimisticLockingFailureException("test conflict"))
                .when(verificationRepository).findLastAttemptNo(50L);
        assertThatThrownBy(() -> service.createVerification(1L, 50L, 60L, "retry-limit"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.CONCURRENT_MODIFICATION));
        verify(verificationRepository, times(3)).findLastAttemptNo(50L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM schedule", Integer.class)).isZero();
    }

    @Test
    void submissionNumberUsesHistoryRatherThanCountedAttempts() {
        var first = service.createVerification(1L, 50L, 60L, "previous");
        // 후속 처리 자체를 테스트하는 것이 아니라, 기술 오류 후 횟수가 복원된 상태를 준비합니다.
        jdbc.update("UPDATE verification SET status='TECH_ERROR' WHERE verification_id=?", first.verificationId());
        jdbc.update("UPDATE schedule SET status='OPEN', attempt_count=0, version=version+1 WHERE schedule_id=50");
        var next = service.createVerification(1L, 50L, 99L, "next");
        assertThat(next.attemptNo()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"})
    void rejectsInvalidMediaHashWithoutChangingSchedule(String hash) {
        jdbc.update("UPDATE media SET file_hash=? WHERE media_id=60", hash);
        assertThatThrownBy(() -> service.createVerification(1L, 50L, 60L, "invalid-hash"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM schedule", Integer.class)).isZero();
        verify(verificationRepository, never()).findLastAttemptNo(50L);
    }

    @Test
    void aiTaskInsertFailureRollsBackVerificationAndSchedule() {
        // 실제 테스트 DB에서 AI 작업 INSERT만 실패시켜 전체 접수의 원자성을 확인합니다.
        jdbc.execute("ALTER TABLE ai_task ADD CONSTRAINT test_ai_task_failure CHECK(task_version=0)");
        try {
            assertThatThrownBy(() -> service.createVerification(1L, 50L, 60L, "ai-task-failure"))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("test_ai_task_failure");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT version FROM schedule", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT status FROM schedule", String.class)).isEqualTo("UPCOMING");
            verifyNoInteractions(client);
        } finally { jdbc.execute("ALTER TABLE ai_task DROP CHECK test_ai_task_failure"); }
        // 실패한 접수의 멱등성 키가 남지 않아 동일 요청으로 다시 정상 접수할 수 있습니다.
        var result = service.createVerification(1L, 50L, 60L, "ai-task-failure");
        assertThat(result.aiTaskId()).isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isEqualTo(1);
    }

    @Test
    void rejectsOtherUsersAndNonVerificationMedia() {
        assertThatThrownBy(() -> service.createVerification(2L, 50L, 60L, "other"))
                .isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE media SET user_id=2 WHERE media_id=60");
        assertThatThrownBy(() -> service.createVerification(1L, 50L, 60L, "other-photo"))
                .isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE media SET user_id=1,purpose='PROFILE' WHERE media_id=60");
        assertThatThrownBy(() -> service.createVerification(1L, 50L, 60L, "profile"))
                .isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isZero();
    }

    @Test
    void rejectsInvalidInputOldDateExhaustedAttemptsAndPendingResult() {
        assertThatThrownBy(() -> service.createVerification(null, 50L, 60L, "key")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.createVerification(1L, 50L, 60L, " ")).isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE schedule SET schedule_date='2020-01-01'");
        assertThatThrownBy(() -> service.createVerification(1L, 50L, 60L, "past")).isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE schedule SET schedule_date=?,attempt_count=3", java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
        assertThatThrownBy(() -> service.createVerification(1L, 50L, 60L, "limit")).isInstanceOf(BusinessException.class);
        jdbc.update("UPDATE schedule SET attempt_count=0,status='PROGRESSING'");
        assertThatThrownBy(() -> service.createVerification(1L, 50L, 60L, "pending")).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isZero();
    }

    @Test
    void scheduleUpdateFailurePreventsVerificationInsert() {
        // 회차 갱신이 먼저 실패하면 인증 INSERT가 실행되지 않고 기존 상태도 유지됩니다.
        jdbc.execute("ALTER TABLE schedule ADD CONSTRAINT test_attempt_failure CHECK(attempt_count=0)");
        try {
            assertThatThrownBy(() -> service.createVerification(1L, 50L, 60L, "rollback"))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("test_attempt_failure");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isZero();
        } finally { jdbc.execute("ALTER TABLE schedule DROP CHECK test_attempt_failure"); }
    }
}
