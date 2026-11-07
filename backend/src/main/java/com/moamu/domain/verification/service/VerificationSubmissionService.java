package com.moamu.domain.verification.service;

import com.moamu.domain.verification.repository.PhotoVerificationRequestRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import org.springframework.stereotype.Service;

@Service
public class VerificationSubmissionService {
    private final PhotoVerificationRequestRepository requestRepository;
    private final PhotoVerificationService photoVerificationService;

    public VerificationSubmissionService(
            PhotoVerificationRequestRepository requestRepository,
            PhotoVerificationService photoVerificationService
    ) {
        this.requestRepository = requestRepository;
        this.photoVerificationService = photoVerificationService;
    }

    // 기존 작업에 연결된 DB 데이터를 조회하고, AI 응답이 올 때까지 동기로 기다린다.
    public PhotoVerificationService.Result verify(Long userId, Long aiTaskId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (aiTaskId == null || aiTaskId <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        var request = requestRepository.findByAiTaskIdAndUserId(aiTaskId, userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        if (request.conditions().isEmpty()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        return photoVerificationService.verify(request);
    }
}
