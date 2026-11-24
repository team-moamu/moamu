package com.moamu.domain.goal.repository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.challenge.exception.ChallengeErrorCode;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalConfirmationSnapshot;
import com.moamu.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GoalAnalysisStore {

    private static final String KEY_PREFIX = "goal:analysis:";
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String LEGACY_LOCK_PREFIX = "goal:confirmation:";
    static final String RECEIPT_PREFIX = "goal:custom-receipt:";
    static final String RESULT_PREFIX = "goal:confirmation-result:";
    static final String ARTWORK_TASK_PREFIX = "goal:artwork-task:";
    private static final String ARTWORK_REPRESENTATIVE_PREFIX = "goal:artwork-representative:";
    static final Duration CONFIRMATION_TTL = Duration.ofDays(7);
    private static final Duration GENERATION_TIMEOUT = Duration.ofSeconds(120);

    public static Instant generationDeadline(Instant acceptedAt) {
        return acceptedAt.plus(GENERATION_TIMEOUT);
    }

    // 분석이 아직 유효하고 먼저 접수된 요청이 없을 때만 새 접수를 저장.
    private static final DefaultRedisScript<Long> ACQUIRE_RECEIPT = new DefaultRedisScript<>("""
            if redis.call('exists', KEYS[1], KEYS[3]) > 0 then return 0 end
            if redis.call('get', KEYS[2]) ~= ARGV[1] then return 0 end
            redis.call('hset', KEYS[1], 'token', ARGV[2], 'analysis', ARGV[1], 'userId', ARGV[3],
                    'startDate', ARGV[4], 'endDate', ARGV[5], 'acceptedAt', ARGV[6], 'expiresAt', ARGV[7],
                    'expiresAtMillis', ARGV[8])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> SAVE_CONFIRMATION = new DefaultRedisScript<>("""
            if redis.call('hget', KEYS[1], 'token') ~= ARGV[1] then return 0 end
            local currentTime = redis.call('time')
            local now = tonumber(currentTime[1]) * 1000 + math.floor(tonumber(currentTime[2]) / 1000)
            local deadline = redis.call('hget', KEYS[1], 'expiresAtMillis')
            if deadline and tonumber(deadline) <= now then return 0 end
            redis.call('set', KEYS[2], ARGV[2])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> FINISH_CONFIRMATION = new DefaultRedisScript<>("""
            if redis.call('hget', KEYS[1], 'token') ~= ARGV[1] then return 0 end
            if redis.call('exists', KEYS[2]) == 0 then return 0 end
            redis.call('hdel', KEYS[1], 'token')
            redis.call('pexpire', KEYS[1], ARGV[2])
            redis.call('pexpire', KEYS[2], ARGV[2])
            return 1
            """, Long.class);

    // 대표 작업을 고르고 대기 관계와 작업 번호를 한 번에 저장.
    private static final DefaultRedisScript<Long> REGISTER_ARTWORK_TASK = new DefaultRedisScript<>("""
            if redis.call('hget', KEYS[1], 'token') ~= ARGV[1] then return 0 end
            if redis.call('hexists', KEYS[1], 'taskId') == 1 then return 0 end
            local now = redis.call('time')
            local nowMillis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
            if tonumber(ARGV[5]) <= nowMillis then return 0 end
            local representative = redis.call('hmget', KEYS[2], 'taskId', 'version')
            if not representative[1] then
                representative = {ARGV[3], ARGV[4]}
                redis.call('hset', KEYS[2], 'taskId', ARGV[3], 'version', ARGV[4])
                redis.call('pexpireat', KEYS[2], ARGV[5])
            end
            redis.call('hset', KEYS[1], 'taskId', ARGV[3], 'taskVersion', ARGV[4],
                    'representativeTaskId', representative[1], 'representativeVersion', representative[2],
                    'representativeKey', KEYS[2], 'expiresAtMillis', ARGV[5])
            redis.call('set', KEYS[3], ARGV[2])
            redis.call('zadd', KEYS[4], nowMillis, ARGV[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 기존 방식으로 확정한 도전도 같은 Redis 키에서 찾음.
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

    public record CustomReceipt(Long userId, LocalDate startDate, LocalDate endDate) {
    }

    public record PendingArtworkTask(Long taskId, int taskVersion, Long representativeTaskId,
            int representativeVersion, Instant acceptedAt, Instant expiresAt) {
    }

    public Optional<PendingArtworkTask> findArtworkTask(String analysisId) {
        List<Object> fields = redisTemplate.opsForHash().multiGet(RECEIPT_PREFIX + analysisId,
                List.of("taskId", "taskVersion", "representativeTaskId", "representativeVersion",
                        "acceptedAt", "expiresAt"));
        if (fields.getFirst() == null) {
            return Optional.empty();
        }
        return Optional.of(new PendingArtworkTask(Long.valueOf((String) fields.get(0)),
                Integer.parseInt((String) fields.get(1)), Long.valueOf((String) fields.get(2)),
                Integer.parseInt((String) fields.get(3)), Instant.parse((String) fields.get(4)),
                Instant.parse((String) fields.get(5))));
    }

    public PendingArtworkTask registerArtworkTask(String analysisId, String token, String categoryKey,
            Long taskId, int taskVersion, Instant acceptedAt) {
        Long registered = redisTemplate.execute(REGISTER_ARTWORK_TASK,
                List.of(RECEIPT_PREFIX + analysisId, ARTWORK_REPRESENTATIVE_PREFIX + categoryKey,
                        ARTWORK_TASK_PREFIX + taskId, ArtworkGenerationWorkStore.QUEUE_KEY),
                token, analysisId, taskId.toString(), Integer.toString(taskVersion),
                Long.toString(generationDeadline(acceptedAt).toEpochMilli()));
        if (!Long.valueOf(1).equals(registered)) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE);
        }
        return findArtworkTask(analysisId)
                .orElseThrow(() -> new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE));
    }

    public Optional<CustomReceipt> findCustomReceipt(String analysisId) {
        List<Object> fields = redisTemplate.opsForHash().multiGet(RECEIPT_PREFIX + analysisId,
                List.of("userId", "startDate", "endDate"));
        if (fields.stream().allMatch(Objects::isNull)) {
            return Optional.empty();
        }
        return Optional.of(new CustomReceipt(Long.valueOf((String) fields.get(0)),
                LocalDate.parse((String) fields.get(1)), LocalDate.parse((String) fields.get(2))));
    }

    // 도전이 DB에 저장됐는지 확인할 때까지 도전 ID를 Redis에 남겨둠.
    public void saveConfirmation(String analysisId, String token, GoalConfirmationSnapshot confirmation) {
        try {
            Long saved = redisTemplate.execute(
                    SAVE_CONFIRMATION,
                    List.of(RECEIPT_PREFIX + analysisId, RESULT_PREFIX + analysisId),
                    token,
                    objectMapper.writeValueAsString(confirmation));
            if (!Long.valueOf(1).equals(saved)) {
                throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("목표 확정 결과를 저장하지 못했습니다.", exception);
        }
    }

    public Optional<String> acquireConfirmation(String analysisId, GoalAnalysisSnapshot snapshot,
            LocalDate startDate, LocalDate endDate, Instant acceptedAt) {
        String rawSnapshot = redisTemplate.opsForValue().get(KEY_PREFIX + analysisId);
        if (rawSnapshot == null || !snapshot.equals(readAnalysis(rawSnapshot))) {
            return Optional.empty();
        }
        String token = UUID.randomUUID().toString();
        Instant expiresAt = generationDeadline(acceptedAt);
        Long acquired = redisTemplate.execute(ACQUIRE_RECEIPT,
                List.of(RECEIPT_PREFIX + analysisId, KEY_PREFIX + analysisId, LEGACY_LOCK_PREFIX + analysisId),
                rawSnapshot, token, snapshot.userId().toString(), startDate.toString(), endDate.toString(),
                acceptedAt.toString(), expiresAt.toString(), Long.toString(expiresAt.toEpochMilli()));
        return Long.valueOf(1).equals(acquired) ? Optional.of(token) : Optional.empty();
    }

    // 도전이 DB에 저장되면 접수 기록과 결과를 7일 동안 보관.
    public void finishConfirmation(String analysisId, String token) {
        redisTemplate.execute(FINISH_CONFIRMATION,
                List.of(RECEIPT_PREFIX + analysisId, RESULT_PREFIX + analysisId), token,
                Long.toString(CONFIRMATION_TTL.toMillis()));
    }

    // 도전 확정에 사용할 분석 결과 조회
    public Optional<GoalAnalysisSnapshot> findByAnalysisId(String analysisId) {
        String analysisJson = redisTemplate.opsForValue().get(KEY_PREFIX + analysisId);
        return Optional.ofNullable(analysisJson).map(this::readAnalysis);
    }

    GoalAnalysisSnapshot readAnalysis(String analysisJson) {
        try {
            GoalAnalysisSnapshot snapshot = objectMapper.readValue(analysisJson, GoalAnalysisSnapshot.class);
            if (snapshot == null) {
                throw new IllegalStateException("목표 분석 결과가 올바르지 않습니다.");
            }
            return snapshot;
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
