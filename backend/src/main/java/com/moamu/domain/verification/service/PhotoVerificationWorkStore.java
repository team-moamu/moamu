package com.moamu.domain.verification.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.domain.verification.dto.PhotoVerificationRequest;

// AI 호출 전 작업을 가져오고, 호출 후 결과를 저장할 권한이 있는지 확인
@Service
public class PhotoVerificationWorkStore {
    public record Claim(long taskId, long verificationId, long userId, String token, int retryCount) {}
    private final JdbcTemplate jdbc;
    private final ScheduleRepository schedules;
    private final VerificationResultService results;
    private final Duration lease;

    // 필요한 객체를 주입하고 AI 통신 제한 시간에 여유 2분을 더해 작업 소유권 유효 시간을 설정
    public PhotoVerificationWorkStore(JdbcTemplate jdbc, ScheduleRepository schedules,
            VerificationResultService results,
            @Value("${app.ai.connect-timeout}") Duration connectTimeout,
            @Value("${app.ai.read-timeout}") Duration readTimeout) {
        this.jdbc = jdbc; this.schedules = schedules; this.results = results;
        this.lease = connectTimeout.plus(readTimeout).plusMinutes(2);
    }

    // 작업 시간 계산에 사용할 현재 한국 시간 반환
    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Seoul")); }

    // 대기 중이거나 재시도할 작업, 처리 시간이 만료된 작업 중 하나를 가져옴
    // 다른 워커가 먼저 가져가지 않은 작업만 RUNNING으로 변경하고 작업 정보와 확인용 토큰을 반환
    // 가져올 작업이 없으면 빈 값을 반환
    @Transactional
    public Optional<Claim> claimNext() {
        var now = now();
        var expired = now.minus(lease);
        var candidates = jdbc.query("""
            SELECT t.ai_task_id, t.verification_id, c.user_id, t.retry_count, t.status
            FROM ai_task t JOIN verification v ON v.verification_id=t.verification_id
            JOIN schedule s ON s.schedule_id=v.schedule_id JOIN challenge c ON c.challenge_id=s.challenge_id
            WHERE t.task_type='VERIFY' AND
              (t.status='PENDING' OR (t.status='RETRYING' AND t.next_retry_at<=?)
               OR (t.status='RUNNING' AND t.locked_by IS NOT NULL AND t.locked_at<?))
            ORDER BY t.ai_task_id LIMIT 20
            """, (row, i) -> new Object[]{row.getLong(1), row.getLong(2), row.getLong(3), row.getInt(4), row.getString(5)}, now, expired);
        for (var row : candidates) {
            String token = UUID.randomUUID().toString();
            int retries = (int) row[3] + ("PENDING".equals(row[4]) ? 0 : 1);
            int changed = jdbc.update("""
                UPDATE ai_task SET status='RUNNING', locked_by=?, locked_at=?, started_at=COALESCE(started_at,?),
                    retry_count=?, next_retry_at=NULL, upd_dt=?
                WHERE ai_task_id=? AND task_type='VERIFY' AND
                  (status='PENDING' OR (status='RETRYING' AND next_retry_at<=?)
                   OR (status='RUNNING' AND locked_by IS NOT NULL AND locked_at<?))
                """, token, now, now, retries, now, row[0], now, expired);
            if (changed == 0) continue;
            jdbc.update("UPDATE verification SET status='ANALYZING', upd_dt=? WHERE verification_id=? AND status='SUBMITTED'", now, row[1]);
            return Optional.of(new Claim((long)row[0], (long)row[1], (long)row[2], token, retries));
        }
        return Optional.empty();
    }

    // 실행 상태, 소유권 토큰, 유효 시간을 확인하고 해당 작업 행을 트랜잭션 종료까지 잠금
    // 소유권이 없거나 만료된 실행은 false를 반환하여 결과 반영을 차단
    private boolean lockOwned(Claim task) {
        return !jdbc.queryForList("""
            SELECT ai_task_id FROM ai_task WHERE ai_task_id=? AND status='RUNNING'
              AND locked_by=? AND locked_at>=? FOR UPDATE
            """, Long.class, task.taskId(), task.token(), now().minus(lease)).isEmpty();
    }

    // 소유권이 유효한 실행만 기존 결과 저장 로직으로 판정과 작업 완료 상태를 저장
    // 소유권이 없으면 저장하지 않고 false를 반환
    @Transactional
    public boolean complete(Claim task, PhotoVerificationRequest request, PhotoVerificationService.Result result) {
        if (!lockOwned(task)) return false;
        jdbc.update("UPDATE ai_task SET locked_by=NULL WHERE ai_task_id=?", task.taskId());
        results.save(request, result);
        return true;
    }

    // 현재 워커가 맡은 작업인지 확인한 뒤 오류를 처리
    // 다시 시도할 수 있으면 다음 실행 시간을 예약
    // 재시도할 수 없거나 횟수를 다 쓰면 FAILED_FINAL/TECH_ERROR로 변경하고 인증 횟수를 한 번 복구
    @Transactional
    public void fail(Claim task, boolean retryable) {
        if (!lockOwned(task)) return;
        var now = now();
        if (retryable && task.retryCount() < 2) {
            jdbc.update("""
                UPDATE ai_task SET status='RETRYING', next_retry_at=?, last_error='AI 일시 오류: 재시도 대기',
                    locked_by=NULL, locked_at=NULL, upd_dt=? WHERE ai_task_id=?
                """, now.plusSeconds(5L * (task.retryCount() + 1)), now, task.taskId());
            return;
        }
        jdbc.update("""
            UPDATE ai_task SET status='FAILED_FINAL', finished_at=?, upd_dt=?,
                last_error='AI 호출 또는 판정 저장 실패', next_retry_at=NULL, locked_by=NULL, locked_at=NULL
            WHERE ai_task_id=?
            """, now, now, task.taskId());
        int changed = jdbc.update("""
            UPDATE verification SET status='TECH_ERROR', upd_dt=?
            WHERE verification_id=? AND status IN ('SUBMITTED','ANALYZING')
            """, now, task.verificationId());
        if (changed == 1) {
            Long scheduleId = jdbc.queryForObject("SELECT schedule_id FROM verification WHERE verification_id=?", Long.class, task.verificationId());
            var schedule = schedules.findById(scheduleId).orElseThrow();
            schedule.restoreTechnicalFailure();
            schedules.flush();
        }
    }
}
