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
    INVALID_CHALLENGE_SCHEDULES(HttpStatus.CONFLICT, "INVALID_CHALLENGE_SCHEDULES", "도전 기간과 회차 정보가 일치하지 않습니다."),
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_NOT_FOUND", "분석 결과가 없거나 만료되었습니다. 목표를 다시 분석해 주세요."),
    ANALYSIS_CONFIRMATION_IN_PROGRESS(HttpStatus.CONFLICT, "ANALYSIS_CONFIRMATION_IN_PROGRESS",
            "같은 분석 결과로 도전을 생성하고 있습니다."),
    ANALYSIS_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "ANALYSIS_ALREADY_CONFIRMED",
            "같은 분석 결과가 다른 시작일 또는 기간으로 이미 확정되었습니다."),
    ARTWORK_TASK_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "ARTWORK_TASK_UNAVAILABLE",
            "일시적으로 도전 접수 상태를 확인할 수 없습니다."),
    ANALYSIS_STALE(HttpStatus.CONFLICT, "ANALYSIS_STALE", "분석에 사용한 기존 카테고리 경로가 비활성화되었습니다. 목표를 다시 분석해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
