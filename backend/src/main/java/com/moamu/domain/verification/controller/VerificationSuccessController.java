package com.moamu.domain.verification.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moamu.domain.verification.dto.VerificationSuccessResponse;
import com.moamu.domain.verification.service.VerificationSuccessService;
import com.moamu.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verification")
public class VerificationSuccessController {

    private final VerificationSuccessService verificationSuccessService;

    // 로그인 사용자의 인증 성공 화면 데이터 조회
    @GetMapping("/{verificationId}/result")
    public ApiResponse<VerificationSuccessResponse> getSuccess(
        @AuthenticationPrincipal Long userId,
        @PathVariable("verificationId") long verificationId
    ) {
        return ApiResponse.success(verificationSuccessService.getSuccess(userId, verificationId));
    }
}
