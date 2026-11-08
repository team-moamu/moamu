package com.moamu.domain.verification;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.media.controller.MediaController;
import com.moamu.domain.media.service.StorageService;
import com.moamu.domain.verification.client.PhotoVerificationClient;
import com.moamu.domain.verification.dto.PhotoVerificationRequest;
import com.moamu.domain.verification.dto.PhotoVerificationResponse;
import com.moamu.domain.verification.enums.CheckStatus;
import com.moamu.domain.verification.exception.PhotoVerificationTechnicalException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 실제 MySQL·서비스·Controller를 연결하고 외부 S3/AI만 대체합니다.
@SpringBootTest(properties = {"jwt.secret=bW9hbXUtdGVzdC1vbmx5LWtleS0zMi1ieXRlcy1sb25nIQ==", "jwt.access-token-expiration=1800000"})
@ActiveProfiles("test")
@Import(BackendApplicationTests.ContainerConfiguration.class)
class MediaVerificationFlowTests {
    private static final long TASK_ID = 9007199254740993L;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.web.context.WebApplicationContext webContext;
    @Autowired com.moamu.global.security.JwtTokenProvider tokens;
    @MockitoBean StorageService storage;
    @MockitoBean PhotoVerificationClient client;
    MockMvc mvc;
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
                INSERT INTO schedule (schedule_id, challenge_id, day_no, schedule_date, piece_index)
                VALUES (50, 40, 1, '2026-11-01', 0)
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
        jdbc.update("DELETE FROM ai_task");
        jdbc.update("DELETE FROM verification");
        jdbc.update("DELETE FROM media");
        jdbc.update("UPDATE schedule SET status='OPEN', schedule_date=?", java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
        jdbc.update("UPDATE challenge SET status='IN_PROGRESS'");
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        reset(storage, client);
        when(storage.saveVerification(any())).thenAnswer(call -> {
            var image = (org.springframework.web.multipart.MultipartFile) call.getArgument(0);
            var hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(image.getBytes()));
            return new com.moamu.domain.media.service.StorageService.StoredFile("verifications/test.png", hash, "image/png", image.getSize());
        });
        when(client.verify(any())).thenAnswer(call -> {
            var request = (PhotoVerificationRequest) call.getArgument(0);
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(jdbc.queryForObject("SELECT status FROM ai_task WHERE ai_task_id=?", String.class, Long.valueOf(request.aiTaskId()))).isEqualTo("RUNNING");
            assertThat(jdbc.queryForObject("SELECT status FROM verification WHERE verification_id=?", String.class, Long.valueOf(request.verificationId()))).isEqualTo("ANALYZING");
            return answer(request, CheckStatus.MET);
        });
    }

    @AfterEach
    void clean() {
        for (String table : List.of("verification_check", "ai_task", "verification", "challenge_condition",
                "schedule", "challenge", "goal_template_artwork", "goal_template_condition", "goal_template",
                "artwork", "media", "goal_category", "users")) jdbc.update("DELETE FROM " + table);
    }


    private PhotoVerificationResponse answer(PhotoVerificationRequest request, CheckStatus status) {
        return new PhotoVerificationResponse(request.aiTaskId(), request.verificationId(), "SUCCEEDED",
                request.conditions().stream().map(c -> new PhotoVerificationResponse.Check(c.code(), status, "판정 사유")).toList(), "test-model");
    }

    private org.springframework.mock.web.MockMultipartFile image() throws Exception {
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
        return new org.springframework.mock.web.MockMultipartFile("image", "test.png", "image/png", out.toByteArray());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder upload(String key) throws Exception {
        return multipart("/api/media").file(image()).param("scheduleId", "50")
                .header("Idempotency-Key", key).header("Authorization", "Bearer " + tokens.createAccessToken(1L));
    }

    @Test
    void successResultRequiresJwtAndChallengeOwnership() throws Exception {
        mvc.perform(upload("success-view")).andExpect(status().isOk());
        var id = jdbc.queryForObject("SELECT verification_id FROM verification", Long.class);
        String path = "/api/verification/" + id + "/result";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path))
                .andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                        .header("Authorization", "Bearer " + tokens.createAccessToken(2L)))
                .andExpect(status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                        .header("Authorization", "Bearer " + tokens.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.earnedPieceCount").value(1))
                .andExpect(jsonPath("$.data.totalPieceCount").value(1))
                .andExpect(jsonPath("$.data.checks.length()").value(2));
        verify(client, times(1)).verify(any());
    }

    @Test
    void singleHttpRequestStoresAllRecordsAndReplayDoesNotCallExternalServers() throws Exception {
        var first = mvc.perform(upload("one")).andExpect(status().isOk())
                .andExpect(jsonPath("data.status").value("SUCCESS"))
                .andExpect(jsonPath("data.verificationId").isNumber())
                .andExpect(jsonPath("data.aiTaskId").isNumber())
                .andExpect(jsonPath("data.checks.length()").value(2)).andReturn().getResponse().getContentAsString();
        mvc.perform(upload("one")).andExpect(status().isOk()).andExpect(content().json(first));
        verify(storage, times(1)).saveVerification(any());
        verify(client, times(1)).verify(any());
        assertThat(jdbc.queryForObject("SELECT status FROM ai_task", String.class)).isEqualTo("SUCCEEDED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification_check", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM schedule", String.class)).isEqualTo("SUCCESS");
        assertThat(jdbc.queryForObject("SELECT version FROM schedule", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForMap("SELECT started_at, finished_at FROM ai_task").values()).doesNotContainNull();
    }

    @ParameterizedTest
    @EnumSource(value=CheckStatus.class, names={"NOT_MET", "UNCERTAIN"})
    void normalNegativeVerdictStillCompletesAiTask(CheckStatus check) throws Exception {
        doAnswer(call -> answer(call.getArgument(0), check)).when(client).verify(any());
        mvc.perform(upload("negative")).andExpect(status().isOk())
                .andExpect(jsonPath("data.status").value(check == CheckStatus.NOT_MET ? "FAILED" : "REVIEW_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT status FROM ai_task", String.class)).isEqualTo("SUCCEEDED");
    }

    @Test
    void failedSubmissionsReopenUntilThirdAttemptThenRejectAnotherSubmission() throws Exception {
        doAnswer(call -> answer(call.getArgument(0), CheckStatus.NOT_MET)).when(client).verify(any());
        for (int attempt = 1; attempt <= 3; attempt++) {
            mvc.perform(upload("attempt-" + attempt)).andExpect(status().isOk())
                    .andExpect(jsonPath("data.status").value("FAILED"));
            assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isEqualTo(attempt);
            assertThat(jdbc.queryForObject("SELECT status FROM schedule", String.class))
                    .isEqualTo(attempt < 3 ? "OPEN" : "FAILED");
        }
        mvc.perform(upload("attempt-4")).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verification", Integer.class)).isEqualTo(3);
        verify(client, times(3)).verify(any());
    }

    @Test
    void aiFailurePreservesPhotoAndRefundsCountOnlyOnce() throws Exception {
        doThrow(new PhotoVerificationTechnicalException("timeout")).when(client).verify(any());
        mvc.perform(upload("failed")).andExpect(status().isBadGateway());
        mvc.perform(upload("failed")).andExpect(status().isBadGateway());
        assertThat(jdbc.queryForObject("SELECT status FROM ai_task", String.class)).isEqualTo("FAILED_FINAL");
        assertThat(jdbc.queryForObject("SELECT status FROM verification", String.class)).isEqualTo("TECH_ERROR");
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM schedule", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM schedule", String.class)).isEqualTo("OPEN");
        verify(storage, never()).delete(any());
        verify(client, times(1)).verify(any());
    }

    @Test
    void rejectedSubmissionCleansNewPhotoAndDoesNotCallAi() throws Exception {
        jdbc.update("UPDATE schedule SET attempt_count=3");
        mvc.perform(upload("limit")).andExpect(status().isBadRequest());
        verify(storage).delete("verifications/test.png");
        verifyNoInteractions(client);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_task", Integer.class)).isZero();
    }

    @Test
    void missingSubmissionFieldsDoNotUploadAnything() throws Exception {
        mvc.perform(multipart("/api/media").file(image()).header("Authorization", "Bearer " + tokens.createAccessToken(1L)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(storage, client);
    }

    @Test
    void simultaneousReplayDoesNotStartAnotherAiCall() throws Exception {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        doAnswer(call -> {
            entered.countDown();
            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("test timeout");
            return answer(call.getArgument(0), CheckStatus.MET);
        }).when(client).verify(any());
        try (var pool = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var first = pool.submit(() -> mvc.perform(upload("concurrent")).andExpect(status().isOk()));
            try {
                assertThat(entered.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                mvc.perform(upload("concurrent")).andExpect(status().isConflict());
            } finally { release.countDown(); }
            first.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        verify(client, times(1)).verify(any());
        verify(storage, times(1)).saveVerification(any());
    }
}
