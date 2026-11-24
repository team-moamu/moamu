package com.moamu.domain.goal.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalConfirmationSnapshot;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ArtworkGenerationWorkStore {

    static final String QUEUE_KEY = "goal:artwork-generation-queue";
    private static final Duration POLL_DELAY = Duration.ofSeconds(2);
    private static final String REDIS_TIME = """
            local time = redis.call('time')
            local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
            """;
    private static final String EXPIRE_TASK = """
            if redis.call('hget', KEYS[1], 'generationState') ~= 'FINALIZING' then
                redis.call('hset', KEYS[1], 'generationState', 'FAILED_FINAL',
                        'generationErrorCode', 'ARTWORK_GENERATION_TIMEOUT', 'generationRetryable', 'true')
                redis.call('pexpire', KEYS[1], retention)
                redis.call('pexpire', taskLookupKey, retention)
            end
            redis.call('hdel', KEYS[1], 'workerToken')
            redis.call('zrem', KEYS[2], ARGV[1])
            return 0
            """;

    // 작업을 가져가면 잠시 숨긴다. 워커가 멈추면 시간이 지난 뒤 다시 가져갈 수 있음.
    private static final DefaultRedisScript<Long> CLAIM = new DefaultRedisScript<>(REDIS_TIME + """
            local score = redis.call('zscore', KEYS[2], ARGV[1])
            if not score or tonumber(score) > now then return 0 end
            local task = redis.call('hmget', KEYS[1], 'taskId', 'taskVersion',
                    'representativeTaskId', 'representativeVersion', 'expiresAtMillis')
            if not task[1] or not task[2] or not task[3] or not task[4] or not task[5] then
                redis.call('zrem', KEYS[2], ARGV[1])
                return 0
            end
            if tonumber(task[5]) <= now then
                local retention = ARGV[4]
                local taskLookupKey = ARGV[5] .. task[1]
            """ + EXPIRE_TASK + """
            end
            local leaseUntil = math.min(now + tonumber(ARGV[3]), tonumber(task[5]))
            redis.call('hset', KEYS[1], 'workerToken', ARGV[2], 'workerLeaseUntil', leaseUntil)
            redis.call('zadd', KEYS[2], leaseUntil, ARGV[1])
            return 1
            """, Long.class);

    // 작업을 넘겨받은 워커만 같은 버전의 다음 상태를 저장할 수 있음.
    private static final DefaultRedisScript<Long> UPDATE = new DefaultRedisScript<>(REDIS_TIME + """
            local task = redis.call('hmget', KEYS[1], 'workerToken', 'workerLeaseUntil',
                    'taskId', 'taskVersion', 'expiresAtMillis', 'representativeKey',
                    'representativeTaskId', 'representativeVersion')
            if task[1] ~= ARGV[2] or task[3] ~= ARGV[3] or task[4] ~= ARGV[4] then return 0 end
            if tonumber(task[5]) <= now then
                local retention = ARGV[9]
                local taskLookupKey = KEYS[4]
            """ + EXPIRE_TASK + """
            end
            if tonumber(task[2]) <= now then return 0 end
            local state = redis.call('hget', KEYS[1], 'generationState')
            if ARGV[5] == 'BEFORE_SUBMIT' then
                if task[3] ~= task[7] or task[4] ~= task[8] then return 0 end
                redis.call('hsetnx', KEYS[1], 'generationRequestBody', ARGV[6])
                redis.call('hdel', KEYS[1], 'generationSubmitAgain')
                return 1
            end
            -- 작품을 배정할 때와 도전을 저장하기 직전에 같은 대표 작업의 결과인지 다시 확인한다.
            if ARGV[5] == 'WAITER_READY' or (ARGV[5] == 'BEFORE_COMMIT' and task[3] ~= task[7]) then
                local representativeReceipt = redis.call('get', KEYS[5])
                if not representativeReceipt or task[3] == task[7] then return 0 end
                local artworkId = ARGV[6]
                if ARGV[5] == 'BEFORE_COMMIT' then
                    artworkId = redis.call('hget', KEYS[1], 'generationArtworkId')
                end
                local representative = redis.call('hmget', ARGV[10] .. representativeReceipt,
                        'taskId', 'taskVersion', 'generationState', 'generationArtworkId', 'generationResult')
                if representative[1] ~= task[7] or representative[2] ~= task[8]
                        or representative[3] ~= 'SUCCEEDED' or not artworkId
                        or representative[4] ~= artworkId then return 0 end
                local valid, result = pcall(cjson.decode, representative[5])
                if not valid or type(result) ~= 'table' or result.reuseAllowed ~= true then return 0 end
            end
            if ARGV[5] == 'BEFORE_COMMIT' then
                if state ~= 'VERIFIED' and state ~= 'FINALIZING' then return 0 end
                redis.call('set', KEYS[3], ARGV[6])
                redis.call('hset', KEYS[1], 'generationState', 'FINALIZING')
                return 1
            end
            if ARGV[5] == 'SUCCEEDED' and state ~= 'FINALIZING' then return 0 end
            redis.call('hdel', KEYS[1], 'workerToken')
            if ARGV[5] == 'VALIDATION_PENDING' then
                redis.call('hset', KEYS[1], 'generationState', ARGV[5], 'generationResult', ARGV[6])
                redis.call('zadd', KEYS[2], now, ARGV[1])
            elseif ARGV[5] == 'VERIFIED' or ARGV[5] == 'WAITER_READY' then
                redis.call('hset', KEYS[1], 'generationState', 'VERIFIED')
                if ARGV[5] == 'WAITER_READY' then
                    redis.call('hset', KEYS[1], 'generationArtworkId', ARGV[6])
                end
                redis.call('zadd', KEYS[2], now, ARGV[1])
            elseif ARGV[5] == 'FAILED_FINAL' or ARGV[5] == 'SUCCEEDED' then
                redis.call('hset', KEYS[1], 'generationState', ARGV[5])
                if ARGV[5] == 'FAILED_FINAL' then
                    redis.call('hset', KEYS[1], 'generationErrorCode', ARGV[6], 'generationRetryable', ARGV[7])
                    redis.call('pexpire', KEYS[1], ARGV[9])
                    redis.call('pexpire', KEYS[4], ARGV[9])
                else
                    redis.call('hset', KEYS[1], 'generationArtworkId', ARGV[6])
                    redis.call('pexpire', KEYS[1], ARGV[9])
                    redis.call('pexpire', KEYS[3], ARGV[9])
                    redis.call('pexpire', KEYS[4], ARGV[9])
                end
                if task[6] then
                    local representative = redis.call('hmget', task[6], 'taskId', 'version')
                    if representative[1] == task[3] and representative[2] == task[4] then
                        redis.call('del', task[6])
                    end
                end
                redis.call('zrem', KEYS[2], ARGV[1])
            else
                if ARGV[5] == 'SUBMIT_AGAIN' then
                    redis.call('hset', KEYS[1], 'generationSubmitAgain', 'true')
                else
                    redis.call('hdel', KEYS[1], 'generationSubmitAgain')
                end
                redis.call('zadd', KEYS[2], math.min(now + tonumber(ARGV[8]), tonumber(task[5])), ARGV[1])
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final GoalAnalysisStore goalAnalysisStore;

    public record ClaimedTask(String analysisId, String token, long taskId, int taskVersion, Instant expiresAt,
            String analysisJson, String requestBody, boolean submitAgain, String resultJson, boolean verified,
            long representativeTaskId, int representativeVersion, Long reusableArtworkId) {
        public boolean isRepresentative() {
            return taskId == representativeTaskId && taskVersion == representativeVersion;
        }
    }

    public enum RepresentativeState {
        WAITING, SUCCEEDED, FAILED, REUSE_DENIED
    }

    public record RepresentativeOutcome(RepresentativeState state, Long artworkId) {
    }

    public Optional<ClaimedTask> claimNext(Instant now, Duration lease) {
        var due = redisTemplate.opsForZSet().rangeByScore(QUEUE_KEY, 0, now.toEpochMilli(), 0, 1);
        if (due == null || due.isEmpty()) {
            return Optional.empty();
        }
        String analysisId = due.iterator().next();
        String receiptKey = GoalAnalysisStore.RECEIPT_PREFIX + analysisId;
        String token = UUID.randomUUID().toString();
        Long claimed = redisTemplate.execute(CLAIM, List.of(receiptKey, QUEUE_KEY), analysisId, token,
                Long.toString(lease.toMillis()), Long.toString(GoalAnalysisStore.CONFIRMATION_TTL.toMillis()),
                GoalAnalysisStore.ARTWORK_TASK_PREFIX);
        if (!Long.valueOf(1).equals(claimed)) {
            return Optional.empty();
        }
        List<Object> fields = redisTemplate.opsForHash().multiGet(receiptKey, List.of("workerToken", "taskId",
                "taskVersion", "expiresAt", "analysis", "generationRequestBody", "generationSubmitAgain",
                "generationResult", "generationState", "representativeTaskId", "representativeVersion",
                "generationArtworkId"));
        if (!token.equals(fields.getFirst())) {
            return Optional.empty();
        }
        return Optional.of(new ClaimedTask(analysisId, token, Long.parseLong((String) fields.get(1)),
                Integer.parseInt((String) fields.get(2)), Instant.parse((String) fields.get(3)),
                (String) fields.get(4), (String) fields.get(5), "true".equals(fields.get(6)),
                (String) fields.get(7), "VERIFIED".equals(fields.get(8)) || "FINALIZING".equals(fields.get(8)),
                Long.parseLong((String) fields.get(9)), Integer.parseInt((String) fields.get(10)),
                fields.get(11) == null ? null : Long.valueOf((String) fields.get(11))));
    }

    public RepresentativeOutcome findRepresentative(ClaimedTask task, Instant now) {
        String analysisId = redisTemplate.opsForValue().get(GoalAnalysisStore.ARTWORK_TASK_PREFIX
                + task.representativeTaskId());
        if (analysisId == null) {
            return new RepresentativeOutcome(RepresentativeState.FAILED, null);
        }
        List<Object> fields = redisTemplate.opsForHash().multiGet(GoalAnalysisStore.RECEIPT_PREFIX + analysisId,
                List.of("taskId", "taskVersion", "generationState", "expiresAtMillis", "generationArtworkId",
                        "generationResult"));
        // 같은 작업 번호라도 재시도 버전이 달라지면 이전 대기 요청에 배정하지 않는다.
        if (!Long.toString(task.representativeTaskId()).equals(fields.get(0))
                || !Integer.toString(task.representativeVersion()).equals(fields.get(1))) {
            return new RepresentativeOutcome(RepresentativeState.FAILED, null);
        }
        if ("SUCCEEDED".equals(fields.get(2))) {
            var result = readResultJson((String) fields.get(5));
            if (!Boolean.TRUE.equals(result.reuseAllowed())) {
                return new RepresentativeOutcome(RepresentativeState.REUSE_DENIED, null);
            }
            return new RepresentativeOutcome(RepresentativeState.SUCCEEDED, Long.valueOf((String) fields.get(4)));
        }
        if ("FAILED_FINAL".equals(fields.get(2)) || now.toEpochMilli() >= Long.parseLong((String) fields.get(3))) {
            return new RepresentativeOutcome(RepresentativeState.FAILED, null);
        }
        return new RepresentativeOutcome(RepresentativeState.WAITING, null);
    }

    public void markWaiterReady(ClaimedTask task, Long artworkId) {
        update(task, "WAITER_READY", artworkId.toString(), false);
    }

    public record FailedTask(String errorCode, boolean retryable) {
    }

    public Optional<FailedTask> findFailure(String analysisId, long taskId, int taskVersion) {
        List<Object> fields = redisTemplate.opsForHash().multiGet(GoalAnalysisStore.RECEIPT_PREFIX + analysisId,
                List.of("taskId", "taskVersion", "generationState", "generationErrorCode", "generationRetryable"));
        if (!Long.toString(taskId).equals(fields.get(0)) || !Integer.toString(taskVersion).equals(fields.get(1))
                || !"FAILED_FINAL".equals(fields.get(2))) {
            return Optional.empty();
        }
        return Optional.of(new FailedTask((String) fields.get(3), "true".equals(fields.get(4))));
    }

    public GoalAnalysisSnapshot readAnalysis(ClaimedTask task) {
        return goalAnalysisStore.readAnalysis(task.analysisJson());
    }

    public ArtworkGenerationResult readResult(ClaimedTask task) {
        return readResultJson(task.resultJson());
    }

    private ArtworkGenerationResult readResultJson(String json) {
        try {
            return objectMapper.readValue(json, ArtworkGenerationResult.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 명화 생성 결과를 읽지 못했습니다.", exception);
        }
    }

    public void markVerified(ClaimedTask task) {
        update(task, "VERIFIED", "", false);
    }

    // 저장 결과를 바로 알 수 없을 때, 같은 요청으로 도전이 이미 만들어졌는지 확인할 수 있게 ID를 남김.
    public boolean beforeCommit(ClaimedTask task, GoalConfirmationSnapshot confirmation) {
        try {
            return update(task, "BEFORE_COMMIT", objectMapper.writeValueAsString(confirmation), false);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("도전 확정 결과를 저장하지 못했습니다.", exception);
        }
    }

    public void markSucceeded(ClaimedTask task, Long artworkId) {
        update(task, "SUCCEEDED", artworkId.toString(), false);
    }

    public boolean beforeSubmit(ClaimedTask task, String body) {
        return update(task, "BEFORE_SUBMIT", body, false);
    }

    public void pollAgain(ClaimedTask task) {
        update(task, "POLL", "", false);
    }

    public void submitAgain(ClaimedTask task) {
        update(task, "SUBMIT_AGAIN", "", false);
    }

    public void fail(ClaimedTask task, String errorCode, boolean retryable) {
        update(task, "FAILED_FINAL", errorCode, retryable);
    }

    public void saveResult(ClaimedTask task, ArtworkGenerationResult result) {
        try {
            update(task, "VALIDATION_PENDING", objectMapper.writeValueAsString(result), false);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("명화 생성 결과를 저장하지 못했습니다.", exception);
        }
    }

    private boolean update(ClaimedTask task, String action, String value, boolean retryable) {
        Long changed = redisTemplate.execute(UPDATE,
                List.of(GoalAnalysisStore.RECEIPT_PREFIX + task.analysisId(), QUEUE_KEY,
                        GoalAnalysisStore.RESULT_PREFIX + task.analysisId(),
                        GoalAnalysisStore.ARTWORK_TASK_PREFIX + task.taskId(),
                        GoalAnalysisStore.ARTWORK_TASK_PREFIX + task.representativeTaskId()),
                task.analysisId(), task.token(), Long.toString(task.taskId()), Integer.toString(task.taskVersion()),
                action, value, Boolean.toString(retryable), Long.toString(POLL_DELAY.toMillis()),
                Long.toString(GoalAnalysisStore.CONFIRMATION_TTL.toMillis()), GoalAnalysisStore.RECEIPT_PREFIX);
        return Long.valueOf(1).equals(changed);
    }
}
