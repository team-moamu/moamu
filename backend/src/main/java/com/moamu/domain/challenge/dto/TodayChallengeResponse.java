package com.moamu.domain.challenge.dto;

import java.time.LocalDate;

import com.moamu.domain.schedule.enums.Status;

public record TodayChallengeResponse(
  Long challengeId,
  String challengeTitle,
  LocalDate startDate,
  LocalDate endDate,
  int dayNo,
  Status scheduleStatus,
  int attemptCount,
  String resultReason
) { }
