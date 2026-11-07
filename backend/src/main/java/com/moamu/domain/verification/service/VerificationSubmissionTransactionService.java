package com.moamu.domain.verification.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moamu.domain.challenge.entity.ChallengeCondition;
import com.moamu.domain.challenge.repository.ChallengeConditionRepository;
import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.media.entity.Media;
import com.moamu.domain.media.enums.MediaPurpose;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.schedule.entity.Schedule;
import com.moamu.domain.schedule.enums.Status;
import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.domain.verification.dto.PhotoVerificationRequest;
import com.moamu.domain.verification.dto.PhotoVerificationResponse;
import com.moamu.domain.verification.dto.VerificationSubmitRequest;
import com.moamu.domain.verification.dto.VerificationSubmitResponse;
import com.moamu.domain.verification.entity.AiTask;
import com.moamu.domain.verification.entity.Verification;
import com.moamu.domain.verification.entity.VerificationCheck;
import com.moamu.domain.verification.enums.VerificationStatus;
import com.moamu.domain.verification.repository.AiTaskRepository;
import com.moamu.domain.verification.repository.VerificationCheckRepository;
import com.moamu.domain.verification.repository.VerificationRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;

import lombok.RequiredArgsConstructor;

// DB작업을 묶어서 처리해아하는 메서드를 담당하는 service
/*
인증하기 클릭
    ↓
prepare()   인증 접수 정보를 DB에 저장
    ↓
AI 판정     다른 서비스에서 실행
    ↓
성공 응답 → complete()   판정 결과 저장
오류 발생 → fail()       기술 오류 기록
*/
@Service
@RequiredArgsConstructor
public class VerificationSubmissionTransactionService {

    private final ScheduleRepository scheduleRepository;
    private final ChallengeRepository challengeRepository;
    private final MediaRepository mediaRepository;
    private final ChallengeConditionRepository conditionRepository;
    private final VerificationRepository verificationRepository;
    private final AiTaskRepository aiTaskRepository;
    private final VerificationCheckRepository checkRepository;

    // 접수 트랜잭션 종료 후 AI 호출에 사용할 정보와 기존 판정 결과
    public record Submission(Long verificationId, Long taskId, int attemptCount,
            PhotoVerificationRequest request, VerificationSubmitResponse completed) {
    }

    // 인증 접수 및 AI 요청 정보 준비
    @Transactional
    public Submission prepare(Long userId, VerificationSubmitRequest request) {
        // 회차 조회 및 잠금. 같은 회차의 동시 제출로 시도 횟수가 중복 증가하는 것을 방지
        Schedule schedule = findSchedule(request.scheduleId());
        // 요청한 도전에 속한 회차인지 확인
        if (!schedule.getChallengeId().equals(request.challengeId())) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
        // 도전 조회 및 소유자 확인
        var challenge = challengeRepository.findById(request.challengeId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        if (!challenge.getUserId().equals(userId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }

        // 중복 제출 방지 키로 기존 접수 확인
        var previous = verificationRepository.findByIdempotencyKey(request.idempotencyKey());
        if (previous.isPresent()) {
            return resume(schedule, previous.get(), request);
        }

        // 제출 가능 여부 확인: 최대 3회, 접수 가능한 상태, 한국 시간 기준 오늘 회차
        if (schedule.getAttemptCount() >= 3
                || (schedule.getStatus() != Status.OPEN && schedule.getStatus() != Status.UPCOMING)
                || !schedule.getScheduleDate().equals(LocalDate.now(ZoneId.of("Asia/Seoul")))) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        // 업로드한 사진 조회 및 소유자와 인증용 사진 여부 확인
        Media media = mediaRepository.findById(request.mediaId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        if (!userId.equals(media.getUserId()) || media.getPurpose() != MediaPurpose.VERIFICATION) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        // 도전 생성 당시 저장한 인증 조건 조회
        List<ChallengeCondition> conditions = findConditions(request.challengeId());

        // 인증 시도 생성 및 저장
        Verification verification = Verification.create(schedule.getScheduleId(), schedule.getAttemptCount() + 1,
                request.idempotencyKey(), media.getFileHash(), media.getMediaId());
        verificationRepository.save(verification);
        // 인증 시도에 연결할 AI 작업 생성 및 저장
        AiTask task = AiTask.createVerification(verification.getVerificationId());
        aiTaskRepository.save(task);
        // 회차의 시도 횟수 증가 및 판정 진행 상태로 변경
        // 조회한 엔티티의 변경은 트랜잭션 종료 시 JPA 변경 감지로 반영
        schedule.startVerification();
        return submission(verification, task, media, conditions);
    }

    // 동일 요청의 기존 결과 반환 또는 기술 오류 재시도 준비
    private Submission resume(Schedule schedule, Verification verification, VerificationSubmitRequest request) {
        // 같은 중복 제출 방지 키를 다른 회차나 사진에 사용하는 요청 차단
        if (!verification.getScheduleId().equals(request.scheduleId())
                || !verification.getMediaId().equals(request.mediaId())) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        // 이미 접수 또는 분석 중이면 AI 요청을 중복 실행하지 않음
        if (verification.getStatus() == VerificationStatus.ANALYZING
                || verification.getStatus() == VerificationStatus.SUBMITTED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        List<ChallengeCondition> conditions = findConditions(schedule.getChallengeId());
        if (verification.getStatus() == VerificationStatus.TECH_ERROR) {
            // 최신 시도만 재시도 허용. 이전 기술 오류가 이후 판정을 덮어쓰는 것을 방지
            if (schedule.getStatus() != Status.OPEN
                    || schedule.getAttemptCount() != verification.getAttemptNo()) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
            }
            // 기존 AI 작업과 사진을 조회하여 같은 인증 기록으로 재시도
            AiTask task = aiTaskRepository.findFirstByVerificationIdOrderByAiTaskIdDesc(
                    verification.getVerificationId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
            Media media = mediaRepository.findById(verification.getMediaId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
            // 재시도 시 새로운 인증 시도를 만들거나 회차의 시도 횟수를 늘리지 않음
            verification.retry();
            task.retry();
            schedule.retryVerification();
            return submission(verification, task, media, conditions);
        }

        // 조건 ID를 코드로 변환하여 저장된 조건별 판정 결과 조회
        Map<Long, String> codes = conditions.stream().collect(Collectors.toMap(
                ChallengeCondition::getChallengeConditionId, ChallengeCondition::getCode));
        List<PhotoVerificationResponse.Check> checks = checkRepository
                .findByVerificationIdOrderByChallengeConditionIdAsc(verification.getVerificationId()).stream()
                .map(check -> new PhotoVerificationResponse.Check(codes.get(check.getChallengeConditionId()),
                        check.getStatus(), check.getReason()))
                .toList();
        return new Submission(verification.getVerificationId(), null, verification.getAttemptNo(), null,
                new VerificationSubmitResponse(verification.getVerificationId(), verification.getStatus().name(),
                        verification.getAttemptNo(), checks));
    }

    // AI 판정 결과와 인증 회차 상태 저장
    @Transactional
    public VerificationSubmitResponse complete(Submission submission, PhotoVerificationService.Result result) {
        // 인증 기록과 AI 작업 조회 및 회차 잠금
        Verification verification = findVerification(submission.verificationId());
        Schedule schedule = findSchedule(verification.getScheduleId());
        AiTask task = findTask(submission.taskId());
        // 조건별 사유를 합쳐 DB 컬럼 길이(500자)에 맞춰 종합 사유 생성
        String reason = result.response().checks().stream().map(PhotoVerificationResponse.Check::reason)
                .collect(Collectors.joining("; "));
        reason = reason.substring(0, Math.min(reason.length(), 500));

        // 인증 결과 및 AI 모델 버전 반영
        verification.complete(result.status(), result.response().modelVersion(), reason);
        // AI 응답의 조건 코드를 DB의 인증 조건 ID에 연결
        Map<String, Long> conditionIds = findConditions(schedule.getChallengeId()).stream()
                .collect(Collectors.toMap(ChallengeCondition::getCode, ChallengeCondition::getChallengeConditionId));
        List<VerificationCheck> checks = result.response().checks().stream()
                .map(check -> VerificationCheck.create(verification.getVerificationId(),
                        conditionIds.get(check.code()), check.status(), check.reason()))
                .toList();
        // 조건별 판정 저장 및 AI 작업 완료 처리
        checkRepository.saveAll(checks);
        task.complete();
        // 성공은 확정, 불확실한 판정은 검토 대기, 실패는 3회 소진 전까지 재인증 허용
        Status status = switch (result.status()) {
            case SUCCESS -> Status.SUCCESS;
            case REVIEW_REQUIRED -> Status.IN_REVIEW;
            case FAILED -> submission.attemptCount() >= 3 ? Status.FAILED : Status.OPEN;
        };
        schedule.completeVerification(status, reason);
        return new VerificationSubmitResponse(verification.getVerificationId(), verification.getStatus().name(),
                submission.attemptCount(), result.response().checks());
    }

    // AI 통신 또는 결과 저장 오류 기록
    @Transactional
    public void fail(Submission submission) {
        Verification verification = findVerification(submission.verificationId());
        Schedule schedule = findSchedule(verification.getScheduleId());
        // 인증 조건 미충족(FAILED)과 구분하여 TECH_ERROR 및 AI 작업 오류로 기록
        verification.fail();
        findTask(submission.taskId()).fail();
        // 동일 요청으로 다시 시도할 수 있도록 회차를 접수 가능 상태로 변경
        schedule.completeVerification(Status.OPEN, verification.getReason());
    }

    // 인증 ID, AI 작업 ID, 사진 키, 인증 조건을 AI 요청 DTO로 변환
    private Submission submission(Verification verification, AiTask task, Media media,
            List<ChallengeCondition> conditions) {
        List<PhotoVerificationRequest.Condition> requestConditions = conditions.stream()
                .map(condition -> new PhotoVerificationRequest.Condition(condition.getCode(), condition.getDescription()))
                .toList();
        return new Submission(verification.getVerificationId(), task.getAiTaskId(), verification.getAttemptNo(),
                new PhotoVerificationRequest(task.getAiTaskId().toString(), verification.getVerificationId().toString(),
                        media.getImageKey(), requestConditions), null);
    }

    // 동시 인증 제출을 막기 위해 비관적 쓰기 잠금으로 회차 조회
    private Schedule findSchedule(Long scheduleId) {
        return scheduleRepository.findForVerification(scheduleId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    // 인증 시도 조회
    private Verification findVerification(Long verificationId) {
        return verificationRepository.findById(verificationId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    // AI 작업 조회
    private AiTask findTask(Long taskId) {
        return aiTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    // AI 판정에 사용할 인증 조건 조회 및 조건 존재 여부 확인
    private List<ChallengeCondition> findConditions(Long challengeId) {
        List<ChallengeCondition> conditions = conditionRepository.findByChallengeIdOrderByChallengeConditionIdAsc(challengeId);
        if (conditions.isEmpty()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        return conditions;
    }
}
