package com.moamu.domain.verification.dto;

// DB 인증 시도 정보
public record VerificationSubmissionResponse(Long verificationId, Long mediaId, int attemptNo, String status) {}
