package com.moamu.domain.verification.service;

import com.moamu.domain.verification.exception.PhotoVerificationTechnicalException;
import com.moamu.domain.verification.repository.VerificationRepository;
import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 작업 확보와 오류 DB 트랜잭션으로 분리
@Service
public class AiTaskExecutionService {
    private final JdbcTemplate jdbc;
    private final VerificationRepository verifications;
    private final ScheduleRepository schedules;

    public AiTaskExecutionService(JdbcTemplate jdbc, VerificationRepository verifications, ScheduleRepository schedules) {
        this.jdbc = jdbc;
        this.verifications = verifications;
        this.schedules = schedules;
    }

    // 조건부 UPDATE로 한 요청만 AI 호출 권한 획득
    // 네트워크 호출 전에 커밋
    @Transactional
    public void start(long taskId, long verificationId) {
        var now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        int changed = jdbc.update("""
                UPDATE ai_task SET status='RUNNING', started_at=?, upd_dt=?
                WHERE ai_task_id=? AND verification_id=? AND task_type='VERIFY' AND status='PENDING'
                """, now, now, taskId, verificationId);
        if (changed != 1) {
            var states = jdbc.queryForList("SELECT status FROM ai_task WHERE ai_task_id=?", String.class, taskId);
            if (states.contains("FAILED_FINAL"))
                throw new PhotoVerificationTechnicalException("이 작업은 기술 오류로 종료되었습니다. 새 요청으로 다시 제출해주세요.");
            throw new BusinessException(CommonErrorCode.CONCURRENT_MODIFICATION);
        }
        if (jdbc.update("""
                UPDATE verification SET status='ANALYZING', upd_dt=?
                WHERE verification_id=? AND status='SUBMITTED'
                """, now, verificationId) != 1)
            throw new PhotoVerificationTechnicalException("분석을 시작할 수 없는 인증 상태입니다.");
    }

    // 자동 재시도가 없는 동기 방식: 기술 오류로 종료하고 사용자 시도 횟수는 한 번만 복구
    @Transactional
    public void fail(long taskId, long verificationId) {
        var now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        int changed = jdbc.update("""
                UPDATE ai_task SET status='FAILED_FINAL', last_error='AI 호출 또는 판정 저장 실패',
                    finished_at=?, upd_dt=?, locked_by=NULL, locked_at=NULL
                WHERE ai_task_id=? AND verification_id=? AND task_type='VERIFY' AND status='RUNNING'
                """, now, now, taskId, verificationId);
        if (changed == 0) return;
        if (jdbc.update("""
                UPDATE verification SET status='TECH_ERROR', upd_dt=?
                WHERE verification_id=? AND status='ANALYZING'
                """, now, verificationId) != 1)
            throw new IllegalStateException("기술 오류를 기록할 수 없는 인증 상태입니다.");
        var verification = verifications.findById(verificationId).orElseThrow();
        var schedule = schedules.findById(verification.getScheduleId()).orElseThrow();
        schedule.restoreTechnicalFailure();
        schedules.flush();
    }
}
