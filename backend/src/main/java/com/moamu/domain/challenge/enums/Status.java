package com.moamu.domain.challenge.enums;

/*
| 상태 | 의미 | 전이 |
| --- | --- | --- |
| `READY` | 생성됨, 시작일 전 | → `IN_PROGRESS` (시작일 도래) |
| `IN_PROGRESS` | 진행 중 | → `SETTLING` (기간 종료), `STOPPED` (사용자 중단) |
| `SETTLING` | 기간 종료, 미확정 회차 또는 작품 준비 대기 | → `COMPLETED` |
| `COMPLETED` | 모든 회차 확정 + 작품 완성·전시 | 종료 |
| `STOPPED` | 사용자 중단 | 종료 |
*/

public enum Status {
    READY,
    IN_PROGRESS,
    SETTLING,
    COMPLETED,
    STOPPED
}
