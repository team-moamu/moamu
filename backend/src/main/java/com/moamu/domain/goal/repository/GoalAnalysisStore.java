package com.moamu.domain.goal.repository;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GoalAnalysisStore {

    private static final String KEY_PREFIX = "goal:analysis:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 사용자별 목표 분석 결과를 Redis에 30분간 저장
    public String save(GoalAnalysisSnapshot snapshot) {
        String analysisId = UUID.randomUUID().toString();

        try {
            String analysisJson = objectMapper.writeValueAsString(snapshot);
            Boolean saved = redisTemplate.opsForValue().setIfAbsent(
                KEY_PREFIX + analysisId,
                analysisJson,
                TTL
            );

            if (!Boolean.TRUE.equals(saved)) {
                throw new IllegalStateException("목표 분석 결과를 저장하지 못했습니다.");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("목표 분석 결과를 직렬화하지 못했습니다.", exception);
        }

        return analysisId;
    }
}
