package com.moamu.domain.challenge.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import com.moamu.domain.challenge.dto.TodayChallengeResponse;
import com.moamu.domain.challenge.entity.Challenge;

import org.springframework.data.repository.query.Param;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

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
        s.resultReason
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
