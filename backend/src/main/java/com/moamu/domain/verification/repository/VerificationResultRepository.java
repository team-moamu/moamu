package com.moamu.domain.verification.repository;

import com.moamu.domain.verification.dto.PhotoVerificationResponse;
import com.moamu.domain.verification.enums.CheckStatus;
import com.moamu.domain.verification.enums.VerificationResult;
import com.moamu.domain.verification.service.PhotoVerificationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// 판정 결과 저장
@Repository
public class VerificationResultRepository {
    private final JdbcTemplate jdbc;

    public VerificationResultRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Condition(long id, String code, String description) {}

    // 해당 인증 시도의 도전의 고정 조건만 조회
    public List<Condition> findConditions(long verificationId) {
        return jdbc.query("""
                SELECT cc.challenge_condition_id, cc.code, cc.description
                FROM verification v
                JOIN schedule s ON s.schedule_id=v.schedule_id
                JOIN challenge_condition cc ON cc.challenge_id=s.challenge_id
                WHERE v.verification_id=? ORDER BY cc.challenge_condition_id
                """, (row, index) -> new Condition(row.getLong(1), row.getString(2), row.getString(3)), verificationId);
    }

    // 이전 시도의 늦은 결과가 새 시도의 회차 상태를 덮어쓰지 않도록 최신 시도만 연결합니다.
    public Optional<Long> findLatestAttemptScheduleId(long verificationId) {
        return jdbc.query("""
                SELECT v.schedule_id FROM verification v
                WHERE v.verification_id=? AND NOT EXISTS (
                    SELECT 1 FROM verification newer
                    WHERE newer.schedule_id=v.schedule_id AND newer.attempt_no>v.attempt_no)
                """, (row, index) -> row.getLong(1), verificationId).stream().findFirst();
    }

    // 동일 작업의 완료 결과 한 번만 반영
    public boolean completeTask(long taskId, long verificationId, LocalDateTime now) {
        return jdbc.update("""
                UPDATE ai_task SET status='SUCCEEDED', finished_at=?, upd_dt=?,
                    last_error=NULL, next_retry_at=NULL, locked_by=NULL, locked_at=NULL
                WHERE ai_task_id=? AND verification_id=? AND task_type='VERIFY'
                  AND status IN ('PENDING', 'RUNNING', 'RETRYING')
                """, now, now, taskId, verificationId) == 1;
    }

    // DB에서 연결한 조건 스냅샷 ID 저장
    public void insertCheck(long verificationId, long conditionId,
            PhotoVerificationResponse.Check check, LocalDateTime now) {
        jdbc.update("""
                INSERT INTO verification_check (verification_id, challenge_condition_id, status, reason, frst_reg_dt)
                VALUES (?, ?, ?, ?, ?)
                """, verificationId, conditionId, check.status().name(), check.reason(), now);
    }

    // 이미 확정된 인증 결과 수정 금지
    public boolean completeVerification(long verificationId, VerificationResult status,
            String reason, String modelVersion, LocalDateTime now) {
        return jdbc.update("""
                UPDATE verification SET status=?, reason=?, model_version=?, analyzed_at=?, upd_dt=?
                WHERE verification_id=? AND status IN ('SUBMITTED', 'ANALYZING', 'TECH_ERROR')
                """, status.name(), reason, modelVersion, now, now, verificationId) == 1;
    }

    // 같은 완료 작업이 다시 전달되면 최초에 저장한 결과를 반환
    public Optional<PhotoVerificationService.Result> findCompleted(long taskId, long verificationId) {
        var results = jdbc.query("""
                SELECT v.status, v.model_version FROM ai_task t
                JOIN verification v ON v.verification_id=t.verification_id
                WHERE t.ai_task_id=? AND t.verification_id=? AND t.task_type='VERIFY'
                  AND t.status='SUCCEEDED' AND v.status IN ('SUCCESS','FAILED','REVIEW_REQUIRED')
                """, (row, index) -> new String[] {row.getString(1), row.getString(2)}, taskId, verificationId);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        var checks = jdbc.query("""
                SELECT cc.code, vc.status, vc.reason FROM verification_check vc
                JOIN challenge_condition cc ON cc.challenge_condition_id=vc.challenge_condition_id
                WHERE vc.verification_id=? ORDER BY cc.challenge_condition_id
                """, (row, index) -> new PhotoVerificationResponse.Check(row.getString(1),
                        CheckStatus.valueOf(row.getString(2)), row.getString(3)), verificationId);
        var saved = results.getFirst();
        return Optional.of(new PhotoVerificationService.Result(VerificationResult.valueOf(saved[0]),
                new PhotoVerificationResponse(Long.toString(taskId), Long.toString(verificationId),
                        "SUCCEEDED", checks, saved[1])));
    }
}
