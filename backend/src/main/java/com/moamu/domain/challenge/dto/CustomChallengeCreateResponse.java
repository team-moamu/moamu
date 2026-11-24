package com.moamu.domain.challenge.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomChallengeCreateResponse(
    Long challengeId,
    Long taskId,
    Integer taskVersion,
    String status,
    Instant acceptedAt,
    Instant expiresAt,
    String errorCode,
    Boolean retryable
) {
    public CustomChallengeCreateResponse(Long challengeId) {
        this(challengeId, null, null, "SUCCEEDED", null, null, null, null);
    }

    public static CustomChallengeCreateResponse pending(Long taskId, int taskVersion,
            Instant acceptedAt, Instant expiresAt) {
        return new CustomChallengeCreateResponse(null, taskId, taskVersion, "PENDING", acceptedAt, expiresAt, null, null);
    }

    public static CustomChallengeCreateResponse running(Long taskId, int taskVersion,
            Instant acceptedAt, Instant expiresAt) {
        return new CustomChallengeCreateResponse(null, taskId, taskVersion, "RUNNING", acceptedAt, expiresAt, null, null);
    }

    public static CustomChallengeCreateResponse failed(Long taskId, int taskVersion, String errorCode, boolean retryable) {
        return new CustomChallengeCreateResponse(null, taskId, taskVersion, "FAILED_FINAL", null, null,
                errorCode, retryable);
    }
}
