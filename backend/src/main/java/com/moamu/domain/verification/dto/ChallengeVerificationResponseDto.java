package com.moamu.domain.verification.dto;

public record ChallengeVerificationResponseDto(
  Long challengeId,
  String challengeTitle,
  Long scheduleId,
  int dayNo,
  int dayCount,
  int attemptCount,
  String description
) {
  
}
