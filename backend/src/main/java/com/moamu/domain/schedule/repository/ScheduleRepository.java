package com.moamu.domain.schedule.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.moamu.domain.schedule.dto.ScheduleDetailResponse;
import com.moamu.domain.schedule.entity.Schedule;
import com.moamu.domain.verification.dto.ChallengeVerificationResponseDto;

import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // 완료 결과 조회용 회차 목록. 종료 처리와 달리 쓰기 잠금을 사용하지 않음
    @Query("""
        SELECT s
        FROM Schedule s
        WHERE s.challengeId = :challengeId
        ORDER BY s.dayNo ASC
        """)
    List<Schedule> findForCompletion(@Param("challengeId") Long challengeId);

    // 판정 저장과 종료 판단이 엇갈리지 않도록 종료 대상 회차를 순서대로 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Schedule s
        WHERE s.challengeId = :challengeId
        ORDER BY s.dayNo ASC
        """)
    List<Schedule> findSchedulesForSettlement(@Param("challengeId") Long challengeId);

    @Query ("""
    SELECT new com.moamu.domain.schedule.dto.ScheduleDetailResponse (
        c.challengeId,
        c.challengeTitle,
        s.scheduleId,
        s.dayNo,
        s.scheduleDate,
        s.status
      )
    FROM Schedule s 
    INNER JOIN Challenge c
        ON c.challengeId = s.challengeId
    WHERE c.userId = :userId
        AND c.challengeId = :challengeId
    ORDER BY s.dayNo DESC
    """)
    List<ScheduleDetailResponse> findByUserIdAndChallengeId(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    
    @Query("""
        SELECT new com.moamu.domain.verification.dto.ChallengeVerificationResponseDto(
            c.challengeId,
            c.challengeTitle,
            s.scheduleId,
            s.dayNo,
            cast(timestampdiff(day, c.startDate, c.endDate) + 1 as Integer),
            s.attemptCount,
            cc.description
        )
        FROM Challenge c
        JOIN Schedule s ON c.challengeId = s.challengeId
        LEFT JOIN ChallengeCondition cc
            ON c.challengeId = cc.challengeId
        WHERE c.challengeId = :challengeId
        AND s.scheduleId = :scheduleId
        """)
    List<ChallengeVerificationResponseDto> findByChallengeIdAndScheduleId(@Param("challengeId") Long challengeId, @Param("scheduleId") Long scheduleId);

    //@Lock: 먼저 조회한 트랜잭션이 해당 회차를 잠금 -> 같은 회차에 인증 요청이 동시에 들어오는 것을 막기 위함.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Schedule s WHERE s.scheduleId = :scheduleId")
    Optional<Schedule> findForVerification(@Param("scheduleId") Long scheduleId);
}
