package com.moamu.domain.verification.dto;

// DB에 등록된 인증 시도, AI 작업 정보
public record VerificationSubmissionResponse(Long verificationId, Long mediaId, int attemptNo, String status, Long aiTaskId) {}
