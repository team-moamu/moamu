package com.moamu.domain.challenge.dto;

import java.time.LocalDate;

import com.moamu.domain.schedule.enums.Status;

public record ChallengeDetailResponse(
    Long challenengeId,
    String challengeTitle,
    Long scheduleId,
    int dayNo,
    LocalDate scheduleDate,
    Status scheduleStatus
) {
    
}
