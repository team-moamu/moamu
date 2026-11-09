package com.moamu.domain.verification.dto;

import java.util.List;

public record VerificationSubmitResponse(
        Long verificationId, String status, int attemptCount,
        List<PhotoVerificationResponse.Check> checks) {
}
