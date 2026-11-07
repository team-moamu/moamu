package com.moamu.domain.verification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moamu.domain.verification.VerificationService;
import com.moamu.domain.verification.dto.ChallengeVerificationResponseDto;
import com.moamu.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<ChallengeVerificationResponseDto>> getVerification(
            @RequestParam("challengeId") Long challengeId,
            @RequestParam("scheduleId") Long scheduleId) {
        return ResponseEntity.ok(ApiResponse.success(
                verificationService.getVerification(challengeId, scheduleId)));
    }
}
