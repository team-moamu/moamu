package com.moamu.domain.schedule.dto;

import java.time.LocalDate;

import com.moamu.domain.schedule.enums.Status;

public record ScheduleDetailResponse(
    Long challengeId,
    String challengeTitle,
    Long scheduleId,
    int dayNo,
    LocalDate scheduleDate,
    Status scheduleStatus
) {
    
}
