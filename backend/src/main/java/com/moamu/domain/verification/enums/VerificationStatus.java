package com.moamu.domain.verification.enums;

// 인증 시도 처리 상태
public enum VerificationStatus {
    // 인증 사진 제출이 정상 접수된 상태
    SUBMITTED,

    // AI가 인증 사진을 분석 중인 상태
    ANALYZING,

    // 모든 인증 조건을 충족한 상태
    SUCCESS,

    // 인증 조건 중 하나 이상을 충족하지 못한 상태
    FAILED,

    // 불충족 조건은 없지만 판단하기 어려운 조건이 있어 검토가 필요한 상태
    REVIEW_REQUIRED,

    // AI 장애·타임아웃·응답 검증 실패 등 기술적 오류가 발생한 상태 (재처리 대상)
    TECH_ERROR
}
