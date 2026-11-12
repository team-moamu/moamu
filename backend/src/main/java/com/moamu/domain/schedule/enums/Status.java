package com.moamu.domain.schedule.enums;

/*
[STATUS 값]

UPCOMING - 미래 회차
OPEN - 인증가능
PROGRESSING - AI 인증 처리 중
IN_REVIEW - 운영 검토 중
SUCCESS - 성공 확정
FAILED - 실패 확정
PENDING_SETTLEMENT - 정산 대기
*/

public enum Status {
    UPCOMING,
    OPEN,
    PROGRESSING,
    IN_REVIEW,
    SUCCESS,
    FAILED,
    PENDING_SETTLEMENT
}
