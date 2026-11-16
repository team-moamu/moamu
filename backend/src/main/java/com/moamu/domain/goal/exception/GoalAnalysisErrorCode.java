package com.moamu.domain.goal.exception;

import org.springframework.http.HttpStatus;

import com.moamu.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoalAnalysisErrorCode implements ErrorCode {

    AI_TECHNICAL_ERROR(
        HttpStatus.BAD_GATEWAY,
        "AI_TECHNICAL_ERROR",
        "목표 분석 중 기술 오류가 발생했습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
