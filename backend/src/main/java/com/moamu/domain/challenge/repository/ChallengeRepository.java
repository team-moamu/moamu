package com.moamu.domain.challenge.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.moamu.domain.challenge.dto.TodayChallengeResponse;
import com.moamu.domain.challenge.entity.Challenge;

import org.springframework.data.repository.query.Param;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

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
