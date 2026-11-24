package com.moamu.domain.challenge;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult;
import com.moamu.domain.artwork.repository.GeneratedArtworkMetadataStore;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalConfirmationSnapshot;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore.ClaimedTask;
import com.moamu.domain.goal.repository.GoalAnalysisStore;
import com.moamu.domain.goal.repository.GoalAnalysisStore.CustomReceipt;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.security.JwtTokenProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// MockMvc에서 실제 Controller·Service·Repository를 거쳐 격리된 MySQL에 저장합니다.
// 테스트 자체는 트랜잭션으로 감싸지 않고 API 종료 후 커밋·롤백 결과를 확인합니다.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BackendApplicationTests.ContainerConfiguration.class)
class ChallengeCreationIntegrationTests {

    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 25);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ChallengeService challengeService;
    @Autowired private JwtTokenProvider tokens;
    @MockitoBean private GoalAnalysisStore goalAnalysisStore;
    @MockitoBean private ArtworkGenerationWorkStore artworkWorkStore;
    @MockitoBean private GeneratedArtworkMetadataStore artworkMetadataStore;
    private boolean failureConstraintAdded;

    @BeforeEach
    void setUp() {
        cleanRows();
        // JWT로 인증할 사용자와 도전 생성에 필요한 데이터를 준비합니다.
        jdbc.update("INSERT INTO users (user_id, name) VALUES (1, 'Test user')");
        jdbc.update("INSERT INTO goal_category (cate_id, cate_code, cate_name) VALUES (10, 'TEST', 'Test')");
        jdbc.update("""
                INSERT INTO goal_template
                    (goal_template_id, cate_id, goal_name, description, photo_guide, verification_scope, status)
                VALUES (20, 10, 'Test goal', 'Practice daily', 'Show result', 'Visible result', 'ACTIVE')
                """);
        jdbc.update("INSERT INTO artwork (artwork_id) VALUES (30)");
        jdbc.update("INSERT INTO goal_template_artwork (goal_template_id, artwork_id) VALUES (20, 30)");
    }

    @AfterEach
    void tearDown() {
        if (failureConstraintAdded) {
            jdbc.execute("ALTER TABLE schedule DROP CHECK ck_test_schedule_day");
            failureConstraintAdded = false;
        }
        cleanRows();
    }

    // 허용 기간의 API 요청에 대해 도전 1건과 기간만큼의 회차가 실제 커밋되는지 확인합니다.
    @ParameterizedTest
    @ValueSource(ints = {7, 8, 14, 30, 90})
    void postCommitsChallengeAndDailySchedules(int days) throws Exception {
        postChallenge(days, 200);

        assertThat(count("challenge")).isEqualTo(1);
        assertThat(count("schedule")).isEqualTo(days);
        Map<String, Object> challenge = jdbc.queryForMap("SELECT * FROM challenge");
        long challengeId = ((Number) challenge.get("challenge_id")).longValue();
        assertThat(((Number) challenge.get("user_id")).longValue()).isEqualTo(1L);
        assertThat(((Number) challenge.get("goal_template_id")).longValue()).isEqualTo(20L);
        assertThat(((Number) challenge.get("artwork_id")).longValue()).isEqualTo(30L);
        assertThat(challenge.get("challenge_title")).isEqualTo("Test goal");
        assertThat(challenge.get("status")).isEqualTo("READY");
        assertThat(challenge.get("frst_reg_dt")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT end_date FROM challenge", java.sql.Date.class).toLocalDate())
                .isEqualTo(START_DATE.plusDays(days - 1));

        // 모든 회차의 도전 연결, 날짜, 번호, 퍼즐 위치와 감사 필드가 DB에도 보존되는지 확인합니다.
        List<Map<String, Object>> schedules = jdbc.queryForList("SELECT * FROM schedule ORDER BY day_no");
        for (int i = 0; i < days; i++) {
            Map<String, Object> schedule = schedules.get(i);
            LocalDate date = START_DATE.plusDays(i);
            assertThat(((Number) schedule.get("challenge_id")).longValue()).isEqualTo(challengeId);
            assertThat(((Number) schedule.get("day_no")).intValue()).isEqualTo(i + 1);
            assertThat(((Number) schedule.get("piece_index")).intValue()).isEqualTo(i);
            assertThat(((java.sql.Date) schedule.get("schedule_date")).toLocalDate()).isEqualTo(date);
            assertThat(schedule.get("status")).isEqualTo("UPCOMING");
            assertThat(schedule.get("frst_reg_dt")).isNotNull();
            assertThat(schedule.get("upd_dt")).isNull();
        }
    }

    // 1·2회차 INSERT 후 3회차에서 실제 DB 오류를 발생시켜, 도전까지 전체 롤백되는지 확인합니다.
    @Test
    void thirdScheduleFailureRollsBackChallengeAndEarlierSchedules() throws Exception {
        // 격리된 테스트 컨테이너에만 적용하며 테스트 종료 시 제거합니다.
        jdbc.execute("ALTER TABLE schedule ADD CONSTRAINT ck_test_schedule_day CHECK (day_no < 3)");
        failureConstraintAdded = true;

        var result = postChallenge(7, 500);
        // 다른 원인으로 500이 발생한 경우 테스트가 잘못 통과하지 않도록 실패 원인도 확인합니다.
        assertThat(result.getResolvedException()).isNotNull();
        assertThat(result.getResolvedException()).hasStackTraceContaining("ck_test_schedule_day");
        assertThat(count("challenge")).isZero();
        assertThat(count("schedule")).isZero();
        assertThat(count("goal_template")).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void finalDeadlineCheckControlsWhetherTheVerifiedGenerationCommits(boolean withinDeadline) {
        jdbc.update("INSERT INTO ai_task (ai_task_id, task_type, task_version, status) "
                + "VALUES (91, 'GENERATE_ARTWORK', 1, 'PENDING')");
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        var task = new ClaimedTask("analysis", "token", 91L, 1, Instant.now().plusSeconds(60),
                "{}", "{}", false, "{}", true, 91L, 1, null);
        when(goalAnalysisStore.findCustomReceipt("analysis"))
                .thenReturn(Optional.of(new CustomReceipt(1L, yesterday, yesterday.plusDays(6))));
        when(artworkWorkStore.readAnalysis(task)).thenReturn(new GoalAnalysisSnapshot(1L, "Walk", "Walk daily",
                null, new CategoryPath("Health", "Exercise", "Walking"), List.of("Shoes"), "Show shoes", "Outdoor"));
        var image = new ArtworkGenerationResult.Image("generated-artworks/91/1/result.webp",
                "image/webp", 100L, "0".repeat(64));
        var source = new ArtworkGenerationResult.Source("work-1", "Painting", "https://example.org/work",
                "PUBLIC_DOMAIN", "https://example.org/rights", Instant.EPOCH);
        when(artworkWorkStore.readResult(task)).thenReturn(new ArtworkGenerationResult(91L, 1,
                ArtworkGenerationResult.Status.SUCCEEDED, null, null, image, source, true));
        when(artworkWorkStore.beforeCommit(eq(task), any(GoalConfirmationSnapshot.class))).thenReturn(withinDeadline);

        if (!withinDeadline) {
            assertThatThrownBy(() -> challengeService.confirmArtworkChallenge(task)).isInstanceOf(BusinessException.class);
            assertThat(count("challenge")).isZero();
            assertThat(count("schedule")).isZero();
            assertThat(count("challenge_condition")).isZero();
            assertThat(count("goal_template_artwork")).isEqualTo(1);
            assertThat(count("artwork")).isEqualTo(1);
            assertThat(count("media")).isZero();
            assertThat(jdbc.queryForObject("SELECT status FROM ai_task WHERE ai_task_id = 91", String.class))
                    .isEqualTo("PENDING");
            return;
        }

        challengeService.confirmArtworkChallenge(task);
        challengeService.confirmArtworkChallenge(task);

        assertThat(count("challenge")).isEqualTo(1);
        assertThat(count("challenge_condition")).isEqualTo(1);
        assertThat(count("schedule")).isEqualTo(7);
        assertThat(count("artwork")).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT purpose FROM media", String.class)).isEqualTo("GENERATED_ARTWORK");
        assertThat(jdbc.queryForObject("SELECT status FROM ai_task WHERE ai_task_id = 91", String.class))
                .isEqualTo("SUCCEEDED");
        assertThat(jdbc.queryForObject("SELECT status FROM schedule WHERE day_no = 1", String.class))
                .isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("SELECT schedule_date FROM schedule WHERE day_no = 1", LocalDate.class))
                .isEqualTo(yesterday);

        Long artworkId = jdbc.queryForObject("SELECT artwork_id FROM ai_task WHERE ai_task_id = 91", Long.class);
        jdbc.update("INSERT INTO ai_task (ai_task_id, task_type, task_version, status) "
                + "VALUES (92, 'GENERATE_ARTWORK', 1, 'PENDING')");
        LocalDate waiterStart = yesterday.plusDays(2);
        var waiter = new ClaimedTask("waiting", "worker", 92L, 1, Instant.now().plusSeconds(60),
                "{}", null, false, null, true, 91L, 1, artworkId);
        when(goalAnalysisStore.findCustomReceipt("waiting"))
                .thenReturn(Optional.of(new CustomReceipt(1L, waiterStart, waiterStart.plusDays(7))));
        when(artworkWorkStore.readAnalysis(waiter)).thenReturn(new GoalAnalysisSnapshot(1L, "Walk", "Walk daily",
                null, new CategoryPath("Health", "Exercise", "Walking"), List.of("Shoes"), "Show shoes", "Outdoor"));
        when(artworkWorkStore.beforeCommit(eq(waiter), any(GoalConfirmationSnapshot.class))).thenReturn(true);

        challengeService.confirmArtworkChallenge(waiter);
        challengeService.confirmArtworkChallenge(waiter);

        assertThat(count("challenge")).isEqualTo(2);
        assertThat(count("artwork")).isEqualTo(2);
        assertThat(count("media")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM challenge WHERE artwork_id = ? AND start_date = ?",
                Integer.class, artworkId, waiterStart)).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.MvcResult postChallenge(int days, int expectedStatus)
            throws Exception {
        return mockMvc.perform(post("/api/challenges")
                .header("Authorization", "Bearer " + tokens.createAccessToken(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"goalTemplateId":20,"startDate":"%s","endDate":"%s"}
                        """.formatted(START_DATE, START_DATE.plusDays(days - 1))))
                .andExpect(status().is(expectedStatus)).andReturn();
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void cleanRows() {
        jdbc.update("DELETE FROM ai_task");
        jdbc.update("DELETE FROM schedule");
        jdbc.update("DELETE FROM challenge_condition");
        jdbc.update("DELETE FROM challenge");
        jdbc.update("DELETE FROM goal_template_artwork");
        jdbc.update("DELETE FROM goal_template_condition");
        jdbc.update("DELETE FROM goal_template");
        jdbc.update("DELETE FROM artwork");
        jdbc.update("DELETE FROM media");
        jdbc.update("UPDATE goal_category SET parent_cate_id = NULL WHERE parent_cate_id IS NOT NULL");
        jdbc.update("DELETE FROM goal_category");
        jdbc.update("DELETE FROM users");
    }
}
