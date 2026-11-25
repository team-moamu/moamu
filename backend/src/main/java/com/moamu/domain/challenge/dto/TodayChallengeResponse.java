package com.moamu.domain.challenge.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
  com.moamu.domain.challenge.enums.Status challengeStatus,
  @JsonIgnore String artworkImageKey,
  String artworkPreviewUrl
) {
  public TodayChallengeResponse(Long challengeId, String challengeTitle, LocalDate startDate, LocalDate endDate,
      Long scheduleId, int dayNo, Status scheduleStatus, int attemptCount, String resultReason,
      Long goalTemplateId, com.moamu.domain.challenge.enums.Status challengeStatus, String artworkImageKey) {
    this(challengeId, challengeTitle, startDate, endDate, scheduleId, dayNo, scheduleStatus, attemptCount,
        resultReason, goalTemplateId, challengeStatus, artworkImageKey, null);
  }

  public TodayChallengeResponse withArtworkPreviewUrl(String previewUrl) {
    return new TodayChallengeResponse(challengeId, challengeTitle, startDate, endDate, scheduleId, dayNo,
        scheduleStatus, attemptCount, resultReason, goalTemplateId, challengeStatus, null, previewUrl);
  }
}
