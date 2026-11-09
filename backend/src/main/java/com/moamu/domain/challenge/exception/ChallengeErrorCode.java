package com.moamu.domain.challenge.exception;

import org.springframework.http.HttpStatus;

import com.moamu.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChallengeErrorCode implements ErrorCode {

    CHALLENGE_NOT_COMPLETED(HttpStatus.CONFLICT, "CHALLENGE_NOT_COMPLETED", "아직 도전 결과가 확정되지 않았습니다."),
    CHALLENGE_NOT_ENDED(HttpStatus.CONFLICT, "CHALLENGE_NOT_ENDED", "아직 도전 기간이 종료되지 않았습니다."),
    CHALLENGE_SETTLEMENT_NOT_ALLOWED(HttpStatus.CONFLICT, "CHALLENGE_SETTLEMENT_NOT_ALLOWED", "종료 처리할 수 없는 도전 상태입니다."),
    INVALID_CHALLENGE_SCHEDULES(HttpStatus.CONFLICT, "INVALID_CHALLENGE_SCHEDULES", "도전 기간과 회차 정보가 일치하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
