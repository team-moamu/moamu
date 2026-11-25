package com.moamu.domain.media.service;

import java.util.List;
import com.moamu.domain.media.dto.MediaResponse;
import com.moamu.domain.verification.repository.VerificationResultRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaVerificationQueryService {
    private final JdbcTemplate jdbc;
    private final VerificationResultRepository results;

    // 로그인한 본인의 접수 상태와 완료 결과만 같은 DB 스냅샷에서 조회한다.
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public MediaResponse get(Long userId, long verificationId) {
        if (userId == null || userId <= 0) throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        var found = jdbc.query("""
            SELECT v.verification_id,v.media_id,t.ai_task_id,v.status FROM verification v
            JOIN ai_task t ON t.verification_id=v.verification_id AND t.task_type='VERIFY'
            JOIN schedule s ON s.schedule_id=v.schedule_id JOIN challenge c ON c.challenge_id=s.challenge_id
            WHERE v.verification_id=? AND c.user_id=? AND c.del_yn='N'
            """, (row,i) -> new MediaResponse(row.getLong(1),row.getLong(2),row.getLong(3),row.getString(4),List.of()), verificationId,userId);
        if (found.isEmpty()) throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        var response = found.getFirst();
        results.findCompleted(response.getAiTaskId(), verificationId).ifPresent(result -> response.setChecks(result.response().checks()));
        return response;
    }
}
