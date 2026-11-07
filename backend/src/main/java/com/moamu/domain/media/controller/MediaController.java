package com.moamu.domain.media.controller;

import com.moamu.domain.media.dto.MediaResponse;
import com.moamu.domain.media.service.MediaVerificationService;
import com.moamu.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// 인증 사진 업로드 요청 받기
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaVerificationService verificationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaResponse>> uploadVerification(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "scheduleId", required = false) Long scheduleId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // JWT 필터가 서명·만료·활성 회원을 검증한 사용자 ID만 사용합니다.
        if (userId == null || userId <= 0) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("AUTHENTICATION_REQUIRED", "로그인이 필요합니다."));
        }

        // 사진 저장 → 인증 접수 → AI 호출 → 판정 저장이 완료된 뒤 최종 결과를 반환
        var response = verificationService.submit(userId, scheduleId, image, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
