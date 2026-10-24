package com.moamu.domain.challenge.enums;

/*
| 판정 | 의미 | 처리 방향 |
| --- | --- | --- |
| `VERIFIABLE` | 사진에서 사전 정의 가능한 행동·결과를 관찰할 수 있음 | 인증 조건 확인 후 생성 |
| `SUBSTITUTE` | 목표 자체를 사진으로 증명하기 어려우나 대체 행동은 확인 가능 | 원래 목표와 대체 인증의 차이를 안내하고 동의 후 생성 |
| `NOT_VERIFIABLE` | 사진으로 관찰할 수 없음 | 목표 수정 안내 |
| `UNSUPPORTED` | 서비스가 지원할 수 없는 요청 | 지원 범위 안내 |
| `REJECTED` | 부적절하거나 위험한 요청 | 생성 차단 |
*/

public enum Verifiability {
    VERIFIABLE,
    SUBSTITUTE,
    NOT_VERIFIABLE,
    UNSUPPORTED,
    REJECTED
}
