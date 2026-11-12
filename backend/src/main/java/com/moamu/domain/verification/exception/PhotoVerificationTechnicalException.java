package com.moamu.domain.verification.exception;

// AI 통신·응답 검증 오류는 사용자의 인증 FAILED 결과와 구분한다.
public class PhotoVerificationTechnicalException extends RuntimeException {

    public PhotoVerificationTechnicalException(String message) {
        super(message);
    }

    public PhotoVerificationTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
