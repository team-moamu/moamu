package com.moamu.domain.verification.dto;

import java.util.List;

public record ChallengeVerificationResponseDto(
  Long challengeId,
  String challengeTitle,
  Long scheduleId,
  int dayNo,
  int dayCount,
  int attemptCount,
  List<String> description
) {
  public ChallengeVerificationResponseDto(
      Long challengeId, String challengeTitle, Long scheduleId,
      int dayNo, int dayCount, int attemptCount, String description) {
    this(challengeId, challengeTitle, scheduleId, dayNo, dayCount, attemptCount,
        description == null ? List.of() : List.of(description));
  }
}
