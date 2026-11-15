package com.moamu.domain.verification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import com.moamu.domain.verification.dto.VerificationSubmitRequest;
import com.moamu.domain.verification.dto.VerificationSubmitResponse;
import com.moamu.domain.verification.service.VerificationSubmissionService;

import com.moamu.domain.verification.service.VerificationService;
import com.moamu.domain.verification.dto.ChallengeVerificationResponseDto;
import com.moamu.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final VerificationSubmissionService submissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<ChallengeVerificationResponseDto>> getVerification(
            @RequestParam("challengeId") Long challengeId,
            @RequestParam("scheduleId") Long scheduleId) {
        return ResponseEntity.ok(ApiResponse.success(
                verificationService.getVerification(challengeId, scheduleId)));
    }

    // 인증하기
    @PostMapping
    public ApiResponse<VerificationSubmitResponse> submitVerification(
            @RequestAttribute(value = "userId", required = false) Long userId,
            @Valid @RequestBody VerificationSubmitRequest request) {
        return ApiResponse.success(submissionService.submit(userId, request));
    }
}
