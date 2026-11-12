package com.moamu.domain.verification.repository;

import com.moamu.domain.verification.dto.PhotoVerificationRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// 기존 DB 기록을 AI 요청 DTO로 조회
@Repository
public class PhotoVerificationRequestRepository {
    private final JdbcTemplate jdbcTemplate;

    public PhotoVerificationRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<PhotoVerificationRequest> findByAiTaskIdAndUserId(Long aiTaskId, Long userId) {
        return jdbcTemplate.query("""
                SELECT t.ai_task_id, v.verification_id, m.image_key, cc.code, cc.description
                FROM ai_task t
                JOIN verification v ON v.verification_id = t.verification_id
                JOIN media m ON m.media_id = v.media_id
                JOIN schedule s ON s.schedule_id = v.schedule_id
                JOIN challenge c ON c.challenge_id = s.challenge_id
                LEFT JOIN challenge_condition cc ON cc.challenge_id = c.challenge_id
                WHERE t.ai_task_id = ? AND t.task_type = 'VERIFY'
                  AND c.user_id = ? AND m.user_id = ? AND m.purpose = 'VERIFICATION'
                ORDER BY cc.challenge_condition_id
                """, rows -> {
            if (!rows.next()) {
                return Optional.empty();
            }
            String taskId = rows.getString("ai_task_id");
            String verificationId = rows.getString("verification_id");
            String imageKey = rows.getString("image_key");
            var conditions = new ArrayList<PhotoVerificationRequest.Condition>();
            do {
                if (rows.getString("code") != null) {
                    conditions.add(new PhotoVerificationRequest.Condition(
                            rows.getString("code"), rows.getString("description")));
                }
            } while (rows.next());
            return Optional.of(new PhotoVerificationRequest(taskId, verificationId, imageKey, List.copyOf(conditions)));
        }, aiTaskId, userId, userId);
    }
}
