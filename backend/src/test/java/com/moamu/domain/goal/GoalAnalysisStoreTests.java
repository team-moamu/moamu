package com.moamu.domain.goal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.repository.GoalAnalysisStore;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalAnalysisStoreTests {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;
    private final ObjectMapper mapper = new ObjectMapper();
    private final GoalAnalysisSnapshot snapshot = new GoalAnalysisSnapshot(7L, "산책하기", null,
            new CategoryPath("건강", "운동", "산책"), List.of("산책로가 보여야 해요."), "길을 촬영해 주세요.", "산책 흔적 확인");
    private GoalAnalysisStore store;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
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
}
