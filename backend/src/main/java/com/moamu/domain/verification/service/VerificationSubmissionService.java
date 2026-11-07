package com.moamu.domain.verification.service;

import com.moamu.domain.verification.exception.PhotoVerificationTechnicalException;

import com.moamu.domain.verification.repository.VerificationResultRepository;

import com.moamu.domain.verification.repository.PhotoVerificationRequestRepository;
import com.moamu.domain.aitask.entity.AiTask;
import com.moamu.domain.aitask.enums.AiTaskType;
import com.moamu.domain.aitask.repository.AiTaskRepository;
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
    private final VerificationResultService resultService;
    private final AiTaskExecutionService executionService;
    private final VerificationResultRepository resultRepository;

    private final VerificationRepository verificationRepository;
    private final AiTaskRepository aiTaskRepository;
    private final ScheduleRepository scheduleRepository;
    private final ChallengeRepository challengeRepository;
    private final MediaRepository mediaRepository;
    private final TransactionTemplate submissionTransaction;

    // 필요한 객체를 주입
    // 인증 접수를 새 트랜잭션에서 처리
    public VerificationSubmissionService(
            PhotoVerificationRequestRepository requestRepository,
            PhotoVerificationService photoVerificationService,
            VerificationResultService resultService,
            AiTaskExecutionService executionService,
            VerificationResultRepository resultRepository,
            VerificationRepository verificationRepository,
            AiTaskRepository aiTaskRepository,
            ScheduleRepository scheduleRepository,
            ChallengeRepository challengeRepository,
            MediaRepository mediaRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.requestRepository = requestRepository;
        this.photoVerificationService = photoVerificationService;
        this.resultService = resultService;
        this.executionService = executionService;
        this.resultRepository = resultRepository;
        this.verificationRepository = verificationRepository;
        this.aiTaskRepository = aiTaskRepository;
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
    // 인증 시도, AI 작업 저장과 회차 갱신을 함께 트랜잭션으로 처리
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
            var task = aiTaskRepository.findByVerificationIdAndTaskType(
                    verification.getVerificationId(), AiTaskType.VERIFY)
                    .orElseThrow(() -> new IllegalStateException("기존 인증 시도에 연결된 AI 작업이 없습니다."));
            return response(verification, task);
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
        // 인증 ID가 생성된 후 대기 작업을 저장
        // 실패시 인증과 회차 변경 롤백
        var task = AiTask.pendingVerification(verification.getVerificationId(), now);
        aiTaskRepository.saveAndFlush(task);
        return response(verification, task);
    }

    // 인증 시도 엔티티 -> 접수 결과 응답 DTO로 변환
    private VerificationSubmissionResponse response(Verification verification, AiTask task) {
        return new VerificationSubmissionResponse(verification.getVerificationId(), verification.getMediaId(),
                verification.getAttemptNo(), verification.getStatus().name(), task.getAiTaskId());
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
        long verificationId = Long.parseLong(request.verificationId());
        // 응답 유실 후 같은 요청이 와도 AI를 다시 호출하지 않고 저장된 결과를 반환
        var completed = resultRepository.findCompleted(aiTaskId, verificationId);
        if (completed.isPresent()) return completed.get();
        executionService.start(aiTaskId, verificationId);
        try {
            if (request.conditions().isEmpty()) {
                throw new PhotoVerificationTechnicalException(
                        "저장된 인증 조건이 없습니다.");
            }
            // RUNNING 커밋 → AI 통신 → 결과 저장(SUCCEEDED)을 순서대로 실행
            var result = photoVerificationService.verify(request);
            return resultService.save(request, result);
        } catch (RuntimeException failure) {
            try {
                executionService.fail(aiTaskId, verificationId);
            } catch (RuntimeException recordingFailure) {
                failure.addSuppressed(recordingFailure);
            }
            throw failure;
        }
    }
}
