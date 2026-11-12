package com.moamu.domain.challenge.dto;

import java.time.LocalDate;

import com.moamu.domain.schedule.enums.Status;

public record TodayChallengeResponse(
  // 응답 DTO
  Long challengeId,
  String challengeTitle,
  LocalDate startDate,
  LocalDate endDate,
  Long scheduleId,
  int dayNo,
  Status scheduleStatus,
  int attemptCount,
  String resultReason,
  Long goalTemplateId,
  com.moamu.domain.challenge.enums.Status challengeStatus
) { }
