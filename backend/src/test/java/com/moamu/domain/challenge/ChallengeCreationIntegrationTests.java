package com.moamu.domain.challenge;

import com.moamu.BackendApplicationTests;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
    private boolean failureConstraintAdded;

    @BeforeEach
    void setUp() {
        cleanRows();
        // 현재 컨트롤러의 임시 사용자 ID(1L)에 맞춰 외래 키가 유효한 데이터를 준비합니다.
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

    // 7·14·30일 API 요청 각각에 대해 도전 1건과 기간만큼의 회차가 실제 커밋되는지 확인합니다.
    @ParameterizedTest
    @ValueSource(ints = {7, 14, 30})
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
            assertThat(schedule.get("start_at"))
                    .isEqualTo(date.atStartOfDay());
            assertThat(schedule.get("deadline_at"))
                    .isEqualTo(date.plusDays(1).atStartOfDay());
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

    private org.springframework.test.web.servlet.MvcResult postChallenge(int days, int expectedStatus)
            throws Exception {
        // 실제 보안 필터를 유지하며 테스트용 인증 정보와 CSRF 토큰을 전달합니다.
        return mockMvc.perform(post("/api/challenges").with(user("test")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"goalTemplateId":20,"durationDays":%d,"startDate":"%s"}
                        """.formatted(days, START_DATE)))
                .andExpect(status().is(expectedStatus)).andReturn();
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void cleanRows() {
        jdbc.update("DELETE FROM schedule");
        jdbc.update("DELETE FROM challenge");
        jdbc.update("DELETE FROM goal_template_artwork");
        jdbc.update("DELETE FROM goal_template");
        jdbc.update("DELETE FROM artwork");
        jdbc.update("DELETE FROM goal_category");
        jdbc.update("DELETE FROM users");
    }
}
