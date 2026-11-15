package com.moamu.domain.goal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalConfirmationSnapshot;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.repository.GoalAnalysisStore;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import com.moamu.global.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalAnalysisStoreTests {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GoalAnalysisSnapshot snapshot = new GoalAnalysisSnapshot(7L, "Daily walk", "산책하기", null,
            new CategoryPath("건강", "운동", "산책"), List.of("산책로가 보여야 해요."), "길을 촬영해 주세요.", "산책 흔적 확인");
    private GoalAnalysisStore store;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(values);
        store = new GoalAnalysisStore(redis, mapper);
    }

    @Test
    void savesUuidSnapshotWithThirtyMinuteExpiry() throws Exception {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        String id = store.save(snapshot);

        var json = ArgumentCaptor.forClass(String.class);
        verify(values).setIfAbsent(eq("goal:analysis:" + id), json.capture(), eq(Duration.ofMinutes(30)));
        assertThat(UUID.fromString(id).toString()).isEqualTo(id);
        assertThat(mapper.readValue(json.getValue(), GoalAnalysisSnapshot.class)).isEqualTo(snapshot);
    }

    @Test
    void doesNotReturnAnalysisIdWhenStorageFails() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> store.save(snapshot)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void readsSnapshotAndTreatsMissingKeyAsExpired() throws Exception {
        when(values.get("goal:analysis:stored-id")).thenReturn(mapper.writeValueAsString(snapshot));

        assertThat(store.findByAnalysisId("stored-id")).contains(snapshot);
        assertThat(store.findByAnalysisId("expired-id")).isEmpty();
    }

    @Test
    void corruptSnapshotDoesNotBecomeAnExpiredAnalysis() {
        when(values.get("goal:analysis:broken-id")).thenReturn("broken json");

        assertThatThrownBy(() -> store.findByAnalysisId("broken-id")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acquiresConfirmationWithUniqueTokenAndExpiry() {
        when(values.setIfAbsent(startsWith("goal:confirmation:"), anyString(), eq(Duration.ofMinutes(30))))
            .thenReturn(true);

        Optional<String> token = store.acquireConfirmation("analysis-id");

        assertThat(token).isPresent();
        assertThat(UUID.fromString(token.orElseThrow()).toString()).isEqualTo(token.orElseThrow());
        verify(values).setIfAbsent("goal:confirmation:analysis-id", token.orElseThrow(), Duration.ofMinutes(30));
    }

    @Test
    void rejectsAlreadyLockedAnalysis() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThat(store.acquireConfirmation("analysis-id")).isEmpty();
    }

    @Test
    void storesConfirmationForSevenDaysWhileCheckingLockOwnership() throws Exception {
        GoalConfirmationSnapshot confirmation = new GoalConfirmationSnapshot(7L, 100L);
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any(), any(), any()))
            .thenReturn(1L);

        store.saveConfirmation("analysis-id", "lock-token", confirmation);

        verify(redis).execute(
            org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
            eq(List.of("goal:confirmation:analysis-id", "goal:confirmation-result:analysis-id")),
            eq("lock-token"),
            eq(mapper.writeValueAsString(confirmation)),
            eq(Long.toString(Duration.ofDays(7).toMillis()))
        );
    }

    @Test
    void expiredLockCannotWriteConfirmation() {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any(), any(), any()))
            .thenReturn(0L);

        assertThatThrownBy(() -> store.saveConfirmation("analysis-id", "old-token", new GoalConfirmationSnapshot(7L, 100L)))
            .isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode().getCode()).isEqualTo("ANALYSIS_CONFIRMATION_IN_PROGRESS"));
    }

    @Test
    void readsSeparateConfirmationAndTreatsExpiredResultAsAbsent() throws Exception {
        GoalConfirmationSnapshot confirmation = new GoalConfirmationSnapshot(7L, 100L);
        when(values.get("goal:confirmation-result:stored-id")).thenReturn(mapper.writeValueAsString(confirmation));

        assertThat(store.findConfirmation("stored-id")).contains(confirmation);
        assertThat(store.findConfirmation("expired-id")).isEmpty();
    }
}
