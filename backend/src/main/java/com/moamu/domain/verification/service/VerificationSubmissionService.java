package com.moamu.domain.verification.service;

import com.moamu.domain.verification.repository.PhotoVerificationRequestRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.moamu.domain.verification.entity.Verification;
import com.moamu.domain.verification.repository.VerificationRepository;
import com.moamu.domain.verification.dto.VerificationSubmissionResponse;
import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.media.enums.MediaPurpose;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;


@Service
public class VerificationSubmissionService {
    private final PhotoVerificationRequestRepository requestRepository;
    private final PhotoVerificationService photoVerificationService;

    private final VerificationRepository verificationRepository;
    private final ScheduleRepository scheduleRepository;
    private final ChallengeRepository challengeRepository;
    private final MediaRepository mediaRepository;
    private final TransactionTemplate submissionTransaction;

    // 필요한 객체를 주입
    // 인증 접수를 새 트랜잭션에서 처리
    public VerificationSubmissionService(
            PhotoVerificationRequestRepository requestRepository,
            PhotoVerificationService photoVerificationService,
            VerificationRepository verificationRepository,
            ScheduleRepository scheduleRepository,
            ChallengeRepository challengeRepository,
            MediaRepository mediaRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.requestRepository = requestRepository;
        this.photoVerificationService = photoVerificationService;
        this.verificationRepository = verificationRepository;
        this.scheduleRepository = scheduleRepository;
        this.challengeRepository = challengeRepository;
        this.mediaRepository = mediaRepository;
        this.submissionTransaction = new TransactionTemplate(transactionManager);
        this.submissionTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.submissionTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // 인증 접수를 처리
    // 낙관적 락 충돌 시 새 트랜잭션으로 최대 3회 시도
    public VerificationSubmissionResponse createVerification(
            Long userId, Long scheduleId, Long mediaId, String idempotencyKey) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return submissionTransaction.execute(transaction ->
                        createInTransaction(userId, scheduleId, mediaId, idempotencyKey));
            } catch (OptimisticLockingFailureException exception) {
                if (attempt == 2) {
                    throw new BusinessException(CommonErrorCode.CONCURRENT_MODIFICATION);
                }
            }
        }
        throw new IllegalStateException("인증 접수 재시도 중 도달할 수 없는 코드에 진입했습니다.");
    }

    // 소유자, 제출 가능 여부, 중복 요청 확인
    // 인증 시도 저장과 회차 갱신을 함께 처리
    private VerificationSubmissionResponse createInTransaction(
            Long userId, Long scheduleId, Long mediaId, String idempotencyKey) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (scheduleId == null || scheduleId <= 0 || mediaId == null || mediaId <= 0
                || idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        // 회차 조회는 동시에 허용
        // 수정 시 다른 요청이 먼저 변경했는지 @Version으로 확인
        var schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        var challenge = challengeRepository.findById(schedule.getChallengeId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        var media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        if (!userId.equals(challenge.getUserId()) || !"N".equals(challenge.getDelYn())
                || !userId.equals(media.getUserId()) || media.getPurpose() != MediaPurpose.VERIFICATION) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        // 같은 요청의 재전송 -> 기존 인증을 반환, 횟수 미차감
        var existing = verificationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            var verification = existing.get();
            if (!scheduleId.equals(verification.getScheduleId()) || !mediaId.equals(verification.getMediaId())) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
            }
            return response(verification);
        }

        var now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        if (!schedule.getScheduleDate().equals(now.toLocalDate())
                || !Set.of("READY", "IN_PROGRESS").contains(challenge.getStatus().name())
                || !Set.of("UPCOMING", "OPEN").contains(schedule.getStatus().name())
                || schedule.getAttemptCount() >= 3) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        if (media.getFileHash() == null || !media.getFileHash().matches("[0-9a-fA-F]{64}")) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        int attemptNo = verificationRepository.findLastAttemptNo(scheduleId) + 1;
        // 인증 INSERT 전 버전 검사 (중복 순번 저장 방지)
        // 인증 저장이 실패하면 트랜잭션 롤백
        schedule.markVerificationSubmitted();
        scheduleRepository.flush();
        var verification = Verification.submitted(scheduleId, mediaId, attemptNo,
                idempotencyKey, media.getFileHash(), now);
        verificationRepository.saveAndFlush(verification);
        return response(verification);
    }

    // 인증 시도 엔티티 -> 접수 결과 응답 DTO로 변환
    private VerificationSubmissionResponse response(Verification verification) {
        return new VerificationSubmissionResponse(verification.getVerificationId(), verification.getMediaId(),
                verification.getAttemptNo(), verification.getStatus().name());
    }

    // AI 작업과 조건을 조회
    // 그 후 AI 호출하여 판정 결과를 반환
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
