package com.moamu.domain.aitask.enums;

public enum AiTaskStatus {
    // DB에 등록되어 실행을 기다리는 상태
    PENDING,

    // AI 작업 실행 중
    RUNNING,

    // AI 처리가 정상 완료된 상태
    SUCCEEDED,

    // 기술 오류 후 재시도 대기
    RETRYING,

    // 더 이상 재시도하지 않는 기술적 실패
    FAILED_FINAL,
    
    // 작업 취소
    CANCELED
}
