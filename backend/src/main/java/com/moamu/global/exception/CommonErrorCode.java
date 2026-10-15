package com.moamu.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor 
public enum CommonErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_REQUEST(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST",
        "요청이 올바르지 않습니다."
    ),

    // 401 Unauthorized
    UNAUTHORIZED(
        HttpStatus.UNAUTHORIZED,
        "UNAUTHORIZED",
        "인증이 필요합니다."
    ),

    // 403 Forbidden
    FORBIDDEN(
        HttpStatus.FORBIDDEN,
        "FORBIDDEN",
        "접근 권한이 없습니다."
    ),

    // 404 Not Found
    RESOURCE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "RESOURCE_NOT_FOUND",
        "요청한 리소스를 찾을 수 없습니다."
    ),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_SERVER_ERROR",
        "서버 오류가 발생했습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}