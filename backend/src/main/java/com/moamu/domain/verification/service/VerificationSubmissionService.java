package com.moamu.domain.verification.service;

import com.moamu.domain.verification.dto.VerificationSubmitRequest;
import com.moamu.domain.verification.dto.VerificationSubmitResponse;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

// 인증 전체 흐름을 관리하는 service
@Service
@RequiredArgsConstructor
public class VerificationSubmissionService {

    private final VerificationSubmissionTransactionService transactionService;
    private final PhotoVerificationService photoVerificationService;

    // 인증 접수 -> AI 판정 요청 -> 판정 결과 저장
    public VerificationSubmitResponse submit(Long userId, VerificationSubmitRequest request) {
        // 로그인 처리에서 전달한 사용자 ID 확인
        if (userId == null || userId <= 0) throw new BusinessException(CommonErrorCode.UNAUTHORIZED);

        // 인증 기록과 AI 작업 생성. 접수 트랜잭션은 AI 호출 전에 종료
        var submission = transactionService.prepare(userId, request);

        // 이미 판정이 완료된 동일 요청이면 저장된 결과 반환
        if (submission.completed() != null) return submission.completed();

        try {
            // DB 트랜잭션 밖에서 AI 서버에 사진 판정 요청
            var result = photoVerificationService.verify(submission.request());

            // AI 판정 결과와 회차 상태를 하나의 트랜잭션으로 저장
            return transactionService.complete(submission, result);
        } catch (RuntimeException exception) {
            // 통신 또는 결과 저장 오류는 인증 실패와 구분하여 기술 오류로 기록
            transactionService.fail(submission);
            throw exception;
        }
    }
}
