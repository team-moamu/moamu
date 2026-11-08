package com.moamu.domain.verification.repository;

import com.moamu.domain.verification.entity.Verification;
import com.moamu.domain.verification.enums.VerificationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationRepository extends JpaRepository<Verification, Long> {

    // 과거의 기술 오류가 이후 확정된 판정을 막지 않도록 회차별 최신 시도만 확인
    @Query("""
        SELECT COUNT(v)
        FROM Verification v
        INNER JOIN Schedule s ON s.scheduleId = v.scheduleId
        WHERE s.challengeId = :challengeId
            AND v.status IN :pendingStatuses
            AND NOT EXISTS (
                SELECT newer.verificationId
                FROM Verification newer
                WHERE newer.scheduleId = v.scheduleId
                    AND newer.attemptNo > v.attemptNo
            )
        """)
    long countPendingLatestVerifications(
        @Param("challengeId") Long challengeId,
        @Param("pendingStatuses") List<VerificationStatus> pendingStatuses
    );
    Optional<Verification> findByIdempotencyKey(String idempotencyKey);

    boolean existsByMediaId(Long mediaId);

    // 인증 시도마다 순번을 부여할 수 있도록 마지막 시도 번호를 조회
    @Query("select coalesce(max(v.attemptNo), 0) from Verification v where v.scheduleId = :scheduleId")
    int findLastAttemptNo(@Param("scheduleId") Long scheduleId);
}
