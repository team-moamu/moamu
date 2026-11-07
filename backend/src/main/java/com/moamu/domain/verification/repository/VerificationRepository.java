package com.moamu.domain.verification.repository;

import com.moamu.domain.verification.entity.Verification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationRepository extends JpaRepository<Verification, Long> {
    Optional<Verification> findByIdempotencyKey(String idempotencyKey);

    boolean existsByMediaId(Long mediaId);

    // 인증 시도마다 순번을 부여할 수 있도록 마지막 시도 번호를 조회
    @Query("select coalesce(max(v.attemptNo), 0) from Verification v where v.scheduleId = :scheduleId")
    int findLastAttemptNo(@Param("scheduleId") Long scheduleId);
}
