package com.moamu.domain.media.service;

import com.moamu.domain.verification.dto.VerificationSubmissionResponse;

import com.moamu.domain.media.dto.MediaResponse;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.verification.repository.VerificationRepository;
import com.moamu.domain.verification.service.VerificationSubmissionService;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// 사진 저장 후 인증 시도와 AI 대기 작업을 등록
@Service
public class MediaVerificationService {
    private final MediaService mediaService;
    private final VerificationSubmissionService submissionService;
    private final VerificationRepository verifications;
    private final MediaRepository media;
    private final MediaVerificationQueryService queryService;

    public MediaVerificationService(MediaService mediaService, VerificationSubmissionService submissionService,
            VerificationRepository verifications, MediaRepository media, MediaVerificationQueryService queryService) {
        this.mediaService = mediaService;
        this.submissionService = submissionService;
        this.verifications = verifications;
        this.media = media;
        this.queryService = queryService;
    }

    // 사진 인증 요청을 처리하고 DB 접수 완료 후 응답
    public MediaResponse submit(Long userId, Long scheduleId, MultipartFile image, String key) {
        if (userId == null || userId <= 0) throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        if (scheduleId == null || scheduleId <= 0 || key == null || key.isBlank() || key.length() > 100)
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);

        // 동일한 멱등성 키로 요청한 기록이 있으면 일치 여부를 검증한 후 기존 인증 정보를 재사용
        var existing = verifications.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            var verification = existing.get();
            var stored = media.findById(verification.getMediaId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
            if (!userId.equals(stored.getUserId()) || !scheduleId.equals(verification.getScheduleId()))
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
            try (var validated = new ImageValidator().validate(image)) {
                if (!validated.fileHash().equals(verification.getFileHash()))
                    throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
            }
            var submitted = submissionService.createVerification(userId, scheduleId, stored.getMediaId(), key);
            return queryService.get(userId, submitted.verificationId());
        }

        // 신규 요청이면 사진을 업로드하고 인증 접수
        var uploaded = mediaService.uploadVerification(userId, image);
        VerificationSubmissionResponse submitted;
        try {
            submitted = submissionService.createVerification(userId, scheduleId, uploaded.getMediaId(), key);
        } catch (RuntimeException failure) {
            // 접수 실패 시 인증에 연결되지 않은 신규 이미지만 정리
            try {
                if (!verifications.existsByMediaId(uploaded.getMediaId()))
                    mediaService.deleteUnsubmitted(uploaded.getMediaId(), userId);
            } catch (RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        return queryService.get(userId, submitted.verificationId());
    }

}
