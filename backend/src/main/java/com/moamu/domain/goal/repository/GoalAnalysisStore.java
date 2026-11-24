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
import com.moamu.domain.challenge.dto.CustomChallengeCreateResponse;
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
    static final String REDIS_TIME = """
            local time = redis.call('time')
            local nowMillis = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
            """;

    // 접수 시각부터 120초 뒤의 만료 시각을 계산.
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
    private static final DefaultRedisScript<Long> SAVE_CONFIRMATION = new DefaultRedisScript<>(REDIS_TIME + """
            if redis.call('hget', KEYS[1], 'token') ~= ARGV[1] then return 0 end
            local deadline = redis.call('hget', KEYS[1], 'expiresAtMillis')
            if deadline and tonumber(deadline) <= nowMillis then return 0 end
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

    // 최초 접수와 재시도에서 같은 방식으로 대표 작업을 고름.
    private static final String ROUTE_ARTWORK_TASK = """
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
            """;
    private static final DefaultRedisScript<Long> REGISTER_ARTWORK_TASK = new DefaultRedisScript<>("""
            if redis.call('hget', KEYS[1], 'token') ~= ARGV[1] then return 0 end
            if redis.call('hexists', KEYS[1], 'taskId') == 1 then return 0 end
            """ + REDIS_TIME + """
            if tonumber(ARGV[5]) <= nowMillis then return 0 end
            """ + ROUTE_ARTWORK_TASK + """
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> PREPARE_RETRY = new DefaultRedisScript<>("""
            if redis.call('hget', KEYS[1], 'taskVersion') ~= ARGV[1]
                    or redis.call('hget', KEYS[1], 'generationState') ~= 'FAILED_FINAL'
                    or redis.call('hget', KEYS[1], 'generationRetryable') ~= 'true' then return 0 end
            if (redis.call('hget', KEYS[1], 'retryPlan') or '') ~= ARGV[2] then return 0 end
            redis.call('hset', KEYS[1], 'retryPlan', ARGV[3])
            -- DB 저장 결과를 확인하기 전에는 재시도 계획이 사라지지 않게 한다.
            redis.call('persist', KEYS[1])
            redis.call('persist', KEYS[2])
            redis.call('del', KEYS[3])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> SAVE_RETRY_CHALLENGE = new DefaultRedisScript<>(REDIS_TIME + """
            if redis.call('hget', KEYS[1], 'taskVersion') ~= ARGV[1]
                    or not redis.call('hget', KEYS[1], 'retryPlan')
                    or tonumber(ARGV[2]) <= nowMillis then return 0 end
            redis.call('set', KEYS[2], ARGV[3])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> PUBLISH_RETRY = new DefaultRedisScript<>(REDIS_TIME + """
            if redis.call('hget', KEYS[1], 'taskVersion') ~= ARGV[1]
                    or not redis.call('hget', KEYS[1], 'retryPlan') then return 0 end
            if ARGV[10] ~= '' and redis.call('exists', KEYS[5]) == 0 then return 0 end
            redis.call('hdel', KEYS[1], 'retryPlan', 'generationState', 'generationErrorCode',
                    'generationRetryable', 'generationArtworkId', 'generationResult',
                    'generationRequestBody', 'generationSubmitAgain', 'workerToken', 'workerLeaseUntil')
            redis.call('hset', KEYS[1], 'taskVersion', ARGV[4], 'acceptedAt', ARGV[6],
                    'expiresAt', ARGV[7], 'expiresAtMillis', ARGV[5], 'retryResult:' .. ARGV[1], ARGV[8])
            if ARGV[10] == '' then
            """ + ROUTE_ARTWORK_TASK + """
            else
                redis.call('hset', KEYS[1], 'generationState', 'SUCCEEDED', 'generationArtworkId', ARGV[10])
                redis.call('pexpire', KEYS[1], ARGV[9])
                redis.call('pexpire', KEYS[3], ARGV[9])
                redis.call('pexpire', KEYS[5], ARGV[9])
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public record RetryPlan(Instant acceptedAt, String categoryKey, Long artworkId) {
        // 이번 재시도의 만료 시각을 계산.
        public Instant expiresAt() {
            return generationDeadline(acceptedAt);
        }
    }

    // 작업 ID로 처음 접수한 분석 ID를 찾음.
    public Optional<String> findTaskAnalysisId(long taskId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(ARTWORK_TASK_PREFIX + taskId));
    }

    // 처음 접수할 때 보관한 분석 결과를 읽음.
    public GoalAnalysisSnapshot readReceiptAnalysis(String analysisId) {
        String json = (String) redisTemplate.opsForHash().get(RECEIPT_PREFIX + analysisId, "analysis");
        if (json == null) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE);
        }
        return readAnalysis(json);
    }

    // DB에 저장하거나 Redis에 반영하는 중인 재시도 계획을 읽음.
    public Optional<RetryPlan> findRetryPlan(String analysisId) {
        return readReceiptField(analysisId, "retryPlan", RetryPlan.class);
    }

    // 같은 실패 버전에 이미 재시도한 적이 있으면 당시 응답을 읽음.
    public Optional<CustomChallengeCreateResponse> findRetryResult(String analysisId, int expectedVersion) {
        return readReceiptField(analysisId, "retryResult:" + expectedVersion, CustomChallengeCreateResponse.class);
    }

    // 접수 기록의 JSON 한 항목을 지정한 타입으로 읽음.
    private <T> Optional<T> readReceiptField(String analysisId, String field, Class<T> type) {
        String json = (String) redisTemplate.opsForHash().get(RECEIPT_PREFIX + analysisId, field);
        try {
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 재시도 정보를 읽지 못했습니다.", exception);
        }
    }

    // 같은 실패 버전이 여전히 재시도 가능한지 확인하고 재시도 계획을 저장.
    public void prepareRetry(String analysisId, long taskId, int version, RetryPlan previous, RetryPlan plan) {
        Long result = redisTemplate.execute(PREPARE_RETRY,
                List.of(RECEIPT_PREFIX + analysisId, ARTWORK_TASK_PREFIX + taskId, RESULT_PREFIX + analysisId),
                Integer.toString(version), previous == null ? "" : writeRetryJson(previous), writeRetryJson(plan));
        if (!Long.valueOf(1).equals(result)) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_RETRY_CONFLICT);
        }
    }

    // 재사용으로 즉시 만든 도전을 나중에 찾을 수 있도록 커밋 전에 ID를 남김.
    public void saveRetryChallenge(String analysisId, int version, RetryPlan plan, GoalConfirmationSnapshot confirmation) {
        Long result = redisTemplate.execute(SAVE_RETRY_CHALLENGE,
                List.of(RECEIPT_PREFIX + analysisId, RESULT_PREFIX + analysisId), Integer.toString(version),
                Long.toString(plan.expiresAt().toEpochMilli()), writeRetryJson(confirmation));
        if (!Long.valueOf(1).equals(result)) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE);
        }
    }

    // DB 커밋 뒤 새 버전과 응답을 저장하고, 이미지 생성이 필요하면 작업을 접수.
    public CustomChallengeCreateResponse publishRetry(String analysisId, long taskId, int version,
            RetryPlan plan, CustomChallengeCreateResponse response) {
        redisTemplate.execute(PUBLISH_RETRY, List.of(RECEIPT_PREFIX + analysisId,
                ARTWORK_REPRESENTATIVE_PREFIX + (plan.categoryKey() == null ? "" : plan.categoryKey()),
                ARTWORK_TASK_PREFIX + taskId, ArtworkGenerationWorkStore.QUEUE_KEY, RESULT_PREFIX + analysisId),
                Integer.toString(version), analysisId, Long.toString(taskId), Integer.toString(version + 1),
                Long.toString(plan.expiresAt().toEpochMilli()), plan.acceptedAt().toString(), plan.expiresAt().toString(),
                writeRetryJson(response), Long.toString(CONFIRMATION_TTL.toMillis()),
                plan.artworkId() == null ? "" : plan.artworkId().toString());
        return findRetryResult(analysisId, version)
                .orElseThrow(() -> new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE));
    }

    // Redis에 저장할 재시도 계획이나 응답을 JSON으로 바꿈.
    private String writeRetryJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("재시도 정보를 저장하지 못했습니다.", exception);
        }
    }

    // 접수에 연결된 도전 ID를 Redis에서 찾음.
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

    // 접수 기록에서 작업 번호, 버전, 대표 작업과 기한을 읽음.
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

    // DB에서 받은 작업 번호를 접수에 연결하고 대표 작업 또는 대기 작업으로 등록.
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

    // 처음 접수한 사용자와 시작일·종료일을 읽음.
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

    // 분석이 유효할 때 최초 접수 기록을 만들고 처리에 사용할 토큰을 돌려줌.
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

    // 저장된 JSON을 분석 결과로 바꾸고 비어 있거나 잘못된 데이터를 걸러냄.
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
