package com.moamu.domain.media.exception;

import com.moamu.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 사진 검증 실패를 공통 예외 처리기에서 400 또는 413으로 응답하기 위한 코드입니다.
@Getter
@RequiredArgsConstructor
public enum MediaErrorCode implements ErrorCode {
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "IMAGE_REQUIRED", "사진을 선택해주세요."),
    IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE", "사진은 10MB 이하여야 합니다."),
    UNSUPPORTED_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "UNSUPPORTED_IMAGE_FORMAT", "JPEG, PNG, WebP 사진만 업로드할 수 있습니다."),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", "올바른 이미지 파일이 아닙니다."),
    IMAGE_RESOLUTION_TOO_LARGE(HttpStatus.BAD_REQUEST, "IMAGE_RESOLUTION_TOO_LARGE", "사진은 2,000만 화소 이하여야 합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
