package com.moamu.domain.challenge.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import com.moamu.domain.challenge.enums.Status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import com.moamu.domain.challenge.dto.TodayChallengeResponse;
import com.moamu.domain.challenge.entity.Challenge;

import org.springframework.data.repository.query.Param;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    // 종료일이 지났거나 마감할 지난 회차가 있는 도전을 ID 순서로 나누어 조회
    @Query("""
        SELECT c
        FROM Challenge c
        WHERE c.challengeId > :afterId
            AND c.delYn = 'N'
            AND c.status IN :statuses
            AND (c.endDate < :today OR EXISTS (
                SELECT s.scheduleId
                FROM Schedule s
                WHERE s.challengeId = c.challengeId
                    AND s.scheduleDate < :today
                    AND s.status IN (
                        com.moamu.domain.schedule.enums.Status.UPCOMING,
                        com.moamu.domain.schedule.enums.Status.OPEN
                    )
            ))
        ORDER BY c.challengeId ASC
        """)
    List<Challenge> findClosingCandidates(
        @Param("today") LocalDate today,
        @Param("afterId") Long afterId,
        @Param("statuses") List<Status> statuses,
        Pageable pageable
    );

    // 완료 화면 조회용. 소유권·삭제 여부만 확인하며 조회 과정에서 상태를 변경하지 않음
    @Query("""
        SELECT c
        FROM Challenge c
        WHERE c.challengeId = :challengeId
            AND c.userId = :userId
            AND c.delYn = 'N'
        """)
    Optional<Challenge> findOwned(
        @Param("userId") Long userId,
        @Param("challengeId") Long challengeId
    );

    // 동일 도전의 종료 요청을 직렬화하며 다른 사용자의 도전은 조회하지 않음
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Challenge c
        WHERE c.challengeId = :challengeId
            AND c.userId = :userId
            AND c.delYn = 'N'
        """)
    Optional<Challenge> findOwnedForSettlement(
        @Param("userId") Long userId,
        @Param("challengeId") Long challengeId
    );

  //JPQL
  @Query ("""
      SELECT new com.moamu.domain.challenge.dto.TodayChallengeResponse (
        c.challengeId,
        c.challengeTitle,
        c.startDate,
        c.endDate,
        s.scheduleId,
        s.dayNo,
        s.status,
        s.attemptCount,
        s.resultReason,
        c.goalTemplateId,
        c.status
      )
      FROM Schedule s
      INNER JOIN Challenge c
          ON s.challengeId = c.challengeId
      WHERE c.userId = :userId
        AND DATE(s.scheduleDate) = :day
      ORDER BY
          CASE WHEN :sort = 'name' THEN c.challengeTitle END ASC,
          CASE WHEN :sort = 'latest' THEN c.challengeId END DESC,
          c.challengeId DESC,
          s.dayNo DESC
    """)
    List<TodayChallengeResponse> findTodayChallenges(@Param("userId") Long userId, @Param("day")LocalDate day, @Param("sort") String sortType);
}
