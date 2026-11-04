package com.moamu.domain.schedule.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.moamu.domain.schedule.dto.ScheduleDetailResponse;
import com.moamu.domain.schedule.entity.Schedule;

import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
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
}
