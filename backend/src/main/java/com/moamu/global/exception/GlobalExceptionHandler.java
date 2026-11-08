package com.moamu.global.exception;

import com.moamu.domain.verification.exception.PhotoVerificationTechnicalException;

import com.moamu.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode.getCode(), errorCode.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        CommonErrorCode errorCode = status.is5xxServerError()
                ? CommonErrorCode.INTERNAL_SERVER_ERROR : CommonErrorCode.INVALID_REQUEST;
        if (status.is5xxServerError()) {
            log.error("Request processing failed", exception);
        }
        return super.handleExceptionInternal(exception,
                ApiResponse.failure(errorCode.getCode(), errorCode.getMessage()), headers, status, request);
    }

    @ExceptionHandler(PhotoVerificationTechnicalException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiTechnicalException(
            PhotoVerificationTechnicalException exception) {
        log.error("AI verification failed", exception);
        return ResponseEntity.status(502).body(ApiResponse.failure(
                "AI_TECHNICAL_ERROR", "AI 판정 처리 중 기술 오류가 발생했습니다. 사용자 인증 실패가 아닙니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unexpected request processing failure", exception);
        CommonErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode.getCode(), errorCode.getMessage()));
    }
}
