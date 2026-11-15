package com.moamu.domain.goal.repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalConfirmationSnapshot;
import com.moamu.domain.challenge.exception.ChallengeErrorCode;
import com.moamu.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GoalAnalysisStore {

    private static final String KEY_PREFIX = "goal:analysis:";
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String LOCK_PREFIX = "goal:confirmation:";
    private static final String RESULT_PREFIX = "goal:confirmation-result:";
    private static final Duration CONFIRMATION_TTL = Duration.ofDays(7);
    private static final DefaultRedisScript<Long> SAVE_CONFIRMATION = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                redis.call('set', KEYS[2], ARGV[2], 'PX', ARGV[3])
                return 1
            end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 확정 결과는 분석 결과의 30분 TTL과 별도로 7일간 보관
    public Optional<GoalConfirmationSnapshot> findConfirmation(String analysisId) {
        String json = redisTemplate.opsForValue().get(RESULT_PREFIX + analysisId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            GoalConfirmationSnapshot confirmation = objectMapper.readValue(json, GoalConfirmationSnapshot.class);
            if (confirmation == null || confirmation.userId() == null || confirmation.challengeId() == null) {
                throw new IllegalStateException("목표 확정 결과가 올바르지 않습니다.");
            }
            return Optional.of(confirmation);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("목표 확정 결과를 읽지 못했습니다.", exception);
        }
    }

    // DB 커밋 전에 결과를 기록. 잠금이 만료된 이전 요청은 저장하지 못하도록 소유권 확인
    public void saveConfirmation(String analysisId, String token, GoalConfirmationSnapshot confirmation) {
        try {
            Long saved = redisTemplate.execute(
                    SAVE_CONFIRMATION,
                    List.of(LOCK_PREFIX + analysisId, RESULT_PREFIX + analysisId),
                    token,
                    objectMapper.writeValueAsString(confirmation),
                    Long.toString(CONFIRMATION_TTL.toMillis()));
            if (!Long.valueOf(1).equals(saved)) {
                throw new BusinessException(ChallengeErrorCode.ANALYSIS_CONFIRMATION_IN_PROGRESS);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("목표 확정 결과를 저장하지 못했습니다.", exception);
        }
    }

    // 같은 분석 ID의 확정 요청은 한 번에 하나만 처리
    public Optional<String> acquireConfirmation(String analysisId) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_PREFIX + analysisId, token, TTL);
        return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
    }

    // 만료 후 다른 요청이 획득한 잠금은 삭제하지 않음
    public void releaseConfirmation(String analysisId, String token) {
        redisTemplate.execute(RELEASE_LOCK, List.of(LOCK_PREFIX + analysisId), token);
    }

    // 도전 확정에 사용할 분석 결과 조회
    public Optional<GoalAnalysisSnapshot> findByAnalysisId(String analysisId) {
        String analysisJson = redisTemplate.opsForValue().get(KEY_PREFIX + analysisId);
        if (analysisJson == null) {
            return Optional.empty();
        }

        try {
            GoalAnalysisSnapshot snapshot = objectMapper.readValue(analysisJson, GoalAnalysisSnapshot.class);
            if (snapshot == null) {
                throw new IllegalStateException("목표 분석 결과가 올바르지 않습니다.");
            }
            return Optional.of(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("목표 분석 결과를 읽지 못했습니다.", exception);
        }
    }

    // 사용자별 목표 분석 결과를 Redis에 30분간 저장
    public String save(GoalAnalysisSnapshot snapshot) {
        String analysisId = UUID.randomUUID().toString();

        try {
            String analysisJson = objectMapper.writeValueAsString(snapshot);
            Boolean saved = redisTemplate.opsForValue().setIfAbsent(
                    KEY_PREFIX + analysisId,
                    analysisJson,
                    TTL);

            if (!Boolean.TRUE.equals(saved)) {
                throw new IllegalStateException("목표 분석 결과를 저장하지 못했습니다.");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("목표 분석 결과를 직렬화하지 못했습니다.", exception);
        }

        return analysisId;
    }
}
