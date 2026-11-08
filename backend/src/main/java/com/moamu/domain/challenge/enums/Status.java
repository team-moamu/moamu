package com.moamu.domain.challenge.enums;

/*
| 상태 | 의미 | 전이 |
| --- | --- | --- |
| `READY` | 생성됨, 시작일 전 | → `IN_PROGRESS` (시작일 도래) |
| `IN_PROGRESS` | 진행 중 | → `SETTLING` (기간 종료), `STOPPED` (사용자 중단) |
| `SETTLING` | 기간 종료, 미확정 회차 또는 인증 결과 대기 | → `COMPLETED` |
| `COMPLETED` | 기간 종료 및 모든 회차 결과 확정. 전시 여부는 isExhibited로 별도 관리 | 종료 |
| `STOPPED` | 사용자 중단 | 종료 |
*/

public enum Status {
    READY,
    IN_PROGRESS,
    SETTLING,
    COMPLETED,
    STOPPED
}
