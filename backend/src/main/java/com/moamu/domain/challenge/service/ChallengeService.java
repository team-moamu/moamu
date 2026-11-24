package com.moamu.domain.challenge.service;

import java.net.URI;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.moamu.domain.aitask.entity.AiTask;
import com.moamu.domain.aitask.enums.AiTaskStatus;
import com.moamu.domain.aitask.enums.AiTaskType;
import com.moamu.domain.aitask.repository.AiTaskRepository;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult;
import com.moamu.domain.artwork.dto.GeneratedArtworkMetadata;
import com.moamu.domain.artwork.entity.Artwork;
import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.artwork.repository.GeneratedArtworkMetadataStore;
import com.moamu.domain.challenge.dto.ChallengeCompletionResponse;
import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
import com.moamu.domain.challenge.dto.ChallengeSettlementResponse;
import com.moamu.domain.challenge.dto.CustomChallengeCreateRequest;
import com.moamu.domain.challenge.dto.CustomChallengeCreateResponse;
import com.moamu.domain.challenge.dto.TodayChallengeResponse;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.challenge.entity.ChallengeCondition;
import com.moamu.domain.challenge.enums.Status;
import com.moamu.domain.challenge.exception.ChallengeErrorCode;
import com.moamu.domain.challenge.repository.ChallengeConditionRepository;
import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalConfirmationSnapshot;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.entity.GoalCategory;
import com.moamu.domain.goal.entity.GoalTemplate;
import com.moamu.domain.goal.entity.GoalTemplateArtwork;
import com.moamu.domain.goal.entity.GoalTemplateCondition;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore.ClaimedTask;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore;
import com.moamu.domain.goal.repository.GoalAnalysisStore;
import com.moamu.domain.goal.repository.GoalCategoryRepository;
import com.moamu.domain.goal.repository.GoalTemplateArtworkRepository;
import com.moamu.domain.goal.repository.GoalTemplateConditionRepository;
import com.moamu.domain.goal.repository.GoalTemplateRepository;
import com.moamu.domain.media.entity.Media;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.schedule.entity.Schedule;
import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.domain.schedule.service.ScheduleService;
import com.moamu.domain.verification.enums.VerificationStatus;
import com.moamu.domain.verification.repository.VerificationRepository;
import com.moamu.domain.verification.service.ArtworkImageUrlService;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ScheduleRepository scheduleRepository;
    private final ChallengeRepository challengeRepository;
    // private final UserRepository userRepository;
    private final GoalTemplateRepository goalTemplateRepository;
    private final ArtworkRepository artworkRepository;
    private final GoalTemplateArtworkRepository goalTemplateArtworkRepository;
    private final GoalTemplateConditionRepository goalTemplateConditionRepository;
    private final ChallengeConditionRepository challengeConditionRepository;
    private final VerificationRepository verificationRepository;
    private final MediaRepository mediaRepository;
    private final ArtworkImageUrlService artworkImageUrlService;
    private final ScheduleService scheduleService;
    private final GoalAnalysisStore goalAnalysisStore;
    private final GoalCategoryRepository goalCategoryRepository;
    private final PlatformTransactionManager transactionManager;
    private final AiTaskRepository aiTaskRepository;
    private final ReusableArtworkService reusableArtworkService;
    private final ArtworkGenerationWorkStore artworkWorkStore;
    private final GeneratedArtworkMetadataStore artworkMetadataStore;

    // 최종 공개 상태와 집계를 동일한 시점으로 조회하며 인증·회차·전시 상태는 변경하지 않음
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ChallengeCompletionResponse getCompletion(Long userId, Long challengeId) {
        // 로그인 사용자와 대상 ID 검증
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (challengeId == null || challengeId <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        Challenge challenge = challengeRepository.findOwned(userId, challengeId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        // 기간이 끝났더라도 정산 전인 도전은 조회로 완료 처리하지 않음
        if (challenge.getStatus() != Status.COMPLETED
                || challenge.getEndDate().isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")))) {
            throw new BusinessException(ChallengeErrorCode.CHALLENGE_NOT_COMPLETED);
        }

        // 완료 상태와 실제 회차가 일치하는지 확인 후 공개 위치 구성
        List<Schedule> schedules = scheduleRepository.findForCompletion(challengeId);
        validateSchedules(challenge, schedules);
        List<Integer> revealedPieceIndices = new ArrayList<>();
        for (Schedule schedule : schedules) {
            switch (schedule.getStatus()) {
                case SUCCESS -> revealedPieceIndices.add(schedule.getPieceIndex());
                case FAILED -> {
                }
                default -> throw new BusinessException(ChallengeErrorCode.CHALLENGE_NOT_COMPLETED);
            }
            if (schedule.getPieceIndex() < 0 || schedule.getPieceIndex() >= schedules.size()) {
                throw new BusinessException(ChallengeErrorCode.INVALID_CHALLENGE_SCHEDULES);
            }
        }
        if (challenge.getGridRows() <= 0 || challenge.getGridCols() <= 0
                || (long) challenge.getGridRows() * challenge.getGridCols() < schedules.size()) {
            throw new BusinessException(ChallengeErrorCode.INVALID_CHALLENGE_SCHEDULES);
        }
        long pendingVerificationCount = verificationRepository.countPendingLatestVerifications(
                challengeId,
                List.of(VerificationStatus.SUBMITTED, VerificationStatus.ANALYZING,
                        VerificationStatus.REVIEW_REQUIRED, VerificationStatus.TECH_ERROR));
        if (pendingVerificationCount > 0) {
            throw new BusinessException(ChallengeErrorCode.CHALLENGE_NOT_COMPLETED);
        }
        revealedPieceIndices.sort(Integer::compareTo);

        // 도전 생성 당시 연결된 작품과 기존 S3 서명 URL 생성 기능 재사용
        Artwork artwork = artworkRepository.findByArtworkIdAndDelYn(challenge.getArtworkId(), "N")
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        String imageKey = artwork.getMediaId() == null ? null
                : mediaRepository.findById(artwork.getMediaId())
                        .map(media -> media.getImageKey()).orElse(null);
        ChallengeCompletionResponse.Artwork artworkResponse = new ChallengeCompletionResponse.Artwork(
                artwork.getArtworkId(), artwork.getArtworkTitle(), artworkImageUrlService.resolve(imageKey));

        // 미획득 수는 전체 회차 수에서 성공 조각 수를 빼서 계산
        int totalPieceCount = schedules.size();
        int earnedPieceCount = revealedPieceIndices.size();
        return new ChallengeCompletionResponse(
                challengeId,
                challenge.getChallengeTitle(),
                challenge.getStatus(),
                challenge.getStartDate(),
                challenge.getEndDate(),
                challenge.getDurationDays(),
                artworkResponse,
                challenge.getGridRows(),
                challenge.getGridCols(),
                revealedPieceIndices,
                totalPieceCount,
                earnedPieceCount,
                totalPieceCount - earnedPieceCount,
                earnedPieceCount == totalPieceCount);
    }

    @Transactional
    public void createChallenge(Long userId, ChallengeCreateRequest request) {

        // 목표 템플릿 조회
        GoalTemplate goalTemplate = goalTemplateRepository
                .findByGoalTemplateIdAndDelYn(request.goalTemplateId(), "N")
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();
        int durationDays = calculateDurationDays(startDate, endDate);

        List<Long> artworkIds = goalTemplateArtworkRepository.findActiveArtworkIds(request.goalTemplateId());
        if (artworkIds.isEmpty()) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
        Long artworkId = artworkIds.get(ThreadLocalRandom.current().nextInt(artworkIds.size()));

        // 퍼즐 그리드
        int[] gridSize = calculateGridSize(durationDays);

        // Challenge 엔티티 생성
        Challenge challenge = Challenge.create(
                userId,
                goalTemplate.getCateId(),
                goalTemplate.getGoalTemplateId(),
                artworkId,
                goalTemplate.getGoalName(),
                goalTemplate.getDescription(),
                null, // 1차 mvp 사용자 입력 도전 제외
                goalTemplate.getPhotoGuide(),
                durationDays,
                startDate,
                endDate,
                goalTemplate.getVerificationScope(),
                gridSize[0],
                gridSize[1]);

        // Challenge 저장
        challengeRepository.save(challenge);

        // 목표 템플릿 인증 조건 조회
        List<GoalTemplateCondition> conditions = goalTemplateConditionRepository
                .findByGoalTemplateIdAndDelYn(request.goalTemplateId(), "N");

        List<ChallengeCondition> challengeConditions = conditions.stream()
                .map(condition -> ChallengeCondition.create(
                        challenge.getChallengeId(),
                        condition.getCode(),
                        condition.getDescription()))
                .toList();
        challengeConditionRepository.saveAll(challengeConditions);

        // 도전 기간에 맞춰 Schedule 생성 및 저장
        createSchedules(challenge.getChallengeId(), startDate, durationDays);
    }

    // 도전 기간에 맞춰 회차 생성
    private void createSchedules(Long challengeId, LocalDate startDate, int durationDays) {
        createSchedules(challengeId, startDate, durationDays, null);
    }

    private void createSchedules(Long challengeId, LocalDate startDate, int durationDays, LocalDateTime confirmedAt) {
        List<Schedule> schedules = new ArrayList<>();
        for (int index = 0; index < durationDays; index++) {
            LocalDate scheduleDate = startDate.plusDays(index);
            Schedule schedule = Schedule.create(
                    challengeId,
                    index + 1,
                    scheduleDate,
                    scheduleDate.atStartOfDay(),
                    scheduleDate.plusDays(1).atStartOfDay(),
                    index);
            if (confirmedAt != null) {
                schedule.closeIfExpired(confirmedAt);
            }
            schedules.add(schedule);
        }
        scheduleRepository.saveAll(schedules);
    }

    // 인증 저장 트랜잭션이 끝난 뒤 호출. 재전송 시에도 완료 처리를 재확인한다.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void completeAfterVerification(Long userId, Long verificationId) {
        var verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        var schedule = scheduleRepository.findById(verification.getScheduleId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        Challenge challenge = challengeRepository.findOwnedForSettlement(userId, schedule.getChallengeId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        // READY에서도 인증을 허용하므로 마지막 회차 확정 시 누락된 시작 상태를 복구한다.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (!challenge.getEndDate().isAfter(today) && schedule.getDayNo() == challenge.getDurationDays()
                && (schedule.getStatus() == com.moamu.domain.schedule.enums.Status.SUCCESS
                        || (schedule.getStatus() == com.moamu.domain.schedule.enums.Status.FAILED
                                && schedule.getAttemptCount() >= 3))) {
            challenge.startIfDue(today);
        }
        settle(userId, schedule.getChallengeId(), true);
    }

    // 명시적 정산은 종료일 다음 날부터 허용한다.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChallengeSettlementResponse settle(Long userId, Long challengeId) {
        return settle(userId, challengeId, false);
    }

    private ChallengeSettlementResponse settle(Long userId, Long challengeId, boolean afterVerification) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (challengeId == null || challengeId <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        // 소유권 및 삭제 여부 확인 후 도전 잠금
        Challenge challenge = challengeRepository.findOwnedForSettlement(userId, challengeId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (afterVerification && (challenge.getEndDate().isAfter(today)
                || (challenge.getStatus() != Status.IN_PROGRESS && challenge.getStatus() != Status.SETTLING
                        && challenge.getStatus() != Status.COMPLETED))) {
            return null;
        }
        if (!afterVerification && !challenge.getEndDate().isBefore(today)) {
            throw new BusinessException(ChallengeErrorCode.CHALLENGE_NOT_ENDED);
        }
        if (challenge.getStatus() != Status.IN_PROGRESS && challenge.getStatus() != Status.SETTLING
                && challenge.getStatus() != Status.COMPLETED) {
            throw new BusinessException(ChallengeErrorCode.CHALLENGE_SETTLEMENT_NOT_ALLOWED);
        }

        // 누락되거나 기간과 맞지 않는 회차로 도전이 완료되는 것을 방지
        List<Schedule> schedules = scheduleRepository.findSchedulesForSettlement(challengeId);
        validateSchedules(challenge, schedules);

        // 마지막 회차 성공 또는 3회 소진 실패에만 당일 완료를 허용한다.
        if (afterVerification) {
            Schedule last = schedules.getLast();
            if (last.getStatus() != com.moamu.domain.schedule.enums.Status.SUCCESS
                    && !(last.getStatus() == com.moamu.domain.schedule.enums.Status.FAILED
                            && last.getAttemptCount() >= 3)) {
                return null;
            }
        }

        // 스케줄러 실행 전 호출돼도 동일한 정책으로 지난 미제출 회차 마감
        // 동일 트랜잭션에 참여하므로 이후 도전 확정이 실패하면 회차 변경도 롤백
        scheduleService.closeExpiredSchedules(userId, challengeId);

        // 마감된 회차 상태로 도전 전체의 확정 여부 집계
        int successCount = 0;
        int failedCount = 0;
        for (Schedule schedule : schedules) {
            switch (schedule.getStatus()) {
                case SUCCESS -> successCount++;
                case FAILED -> failedCount++;
                default -> {
                }
            }
        }
        int pendingCount = schedules.size() - successCount - failedCount;

        // 기술 오류 재처리 정책은 미정이므로 최신 TECH_ERROR도 완료 대기 대상으로 유지
        long pendingVerificationCount = verificationRepository.countPendingLatestVerifications(
                challengeId,
                List.of(VerificationStatus.SUBMITTED, VerificationStatus.ANALYZING,
                        VerificationStatus.REVIEW_REQUIRED, VerificationStatus.TECH_ERROR));
        boolean allFinalized = pendingCount == 0 && pendingVerificationCount == 0;

        // 이미 완료된 도전은 그대로 반환. 잘못된 확정 상태를 되돌리거나 덮어쓰지 않음
        if (challenge.getStatus() == Status.COMPLETED) {
            if (!allFinalized) {
                throw new BusinessException(ChallengeErrorCode.CHALLENGE_SETTLEMENT_NOT_ALLOWED);
            }
        } else if (allFinalized || !afterVerification) {
            challenge.settle(allFinalized);
        }

        return new ChallengeSettlementResponse(
                challengeId,
                challenge.getStatus(),
                challenge.getEndDate(),
                challenge.getDurationDays(),
                schedules.size(),
                successCount,
                failedCount,
                pendingCount,
                pendingVerificationCount,
                allFinalized);
    }

    // 종료 처리 전 도전 기간과 회차의 개수·날짜·순번 검증
    private void validateSchedules(Challenge challenge, List<Schedule> schedules) {
        if (schedules.size() != challenge.getDurationDays()
                || !challenge.getStartDate().plusDays(challenge.getDurationDays() - 1L)
                        .equals(challenge.getEndDate())) {
            throw new BusinessException(ChallengeErrorCode.INVALID_CHALLENGE_SCHEDULES);
        }
        for (int index = 0; index < schedules.size(); index++) {
            Schedule schedule = schedules.get(index);
            if (schedule.getDayNo() != index + 1
                    || !schedule.getScheduleDate().equals(challenge.getStartDate().plusDays(index))) {
                throw new BusinessException(ChallengeErrorCode.INVALID_CHALLENGE_SCHEDULES);
            }
        }
    }

    private int calculateDurationDays(LocalDate startDate, LocalDate endDate) {
        long durationDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (durationDays < 7 || durationDays > 90) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        return (int) durationDays;
    }

    // 퍼즐 그리드 계산
    private int[] calculateGridSize(int durationDays) {
        int cols = (int) Math.ceil(Math.sqrt(durationDays));
        int rows = (int) Math.ceil((double) durationDays / cols);

        return new int[] { rows, cols };
    }

    // 오늘의 도전 조회
    public List<TodayChallengeResponse> getTodayChallenges(Long userId, LocalDate day, String sort) {
        // 사용자 조회 -> 회원 기능 완료되면 적용
        // User user = userRepository.findById(userId)
        // .orElseThrow(() ->
        // new IllegalArgumentException("존재하지 않는 회원입니다.")
        // );

        // 파라미터로 받는 날짜 없으면 오늘 날짜로 처리
        LocalDate targetDate = day != null ? day : LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 정렬 기준
        // 최신순 - 기본값(내림차순) / 이름순(오름차순)
        String sortType = "name".equals(sort) ? "name" : "latest";

        return challengeRepository.findTodayChallenges(userId, targetDate, sortType);
    }

    // 자유목표 도전 확정
    public CustomChallengeCreateResponse createCustomChallenge(Long userId, CustomChallengeCreateRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (request == null || request.analysisId() == null || request.startDate() == null
                || request.endDate() == null) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        int durationDays = calculateDurationDays(request.startDate(), request.endDate());

        String analysisId;
        try {
            analysisId = UUID.fromString(request.analysisId()).toString();
            if (!analysisId.equalsIgnoreCase(request.analysisId())) {
                throw new IllegalArgumentException("Invalid analysis ID");
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        try {
            return receiveCustomChallenge(analysisId, userId, request, durationDays);
        } catch (RedisConnectionFailureException | RedisSystemException exception) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE, exception);
        }
    }

    private CustomChallengeCreateResponse receiveCustomChallenge(String analysisId, Long userId,
            CustomChallengeCreateRequest request, int durationDays) {
        Instant acceptedAt = Instant.now();
        Optional<CustomChallengeCreateResponse> confirmed = findReceivedChallenge(analysisId, userId, request);
        if (confirmed.isPresent()) {
            return confirmed.get();
        }

        var liveAnalysis = goalAnalysisStore.findByAnalysisId(analysisId);
        if (liveAnalysis.isEmpty()) {
            return findReceivedChallenge(analysisId, userId, request)
                    .orElseThrow(() -> new BusinessException(ChallengeErrorCode.ANALYSIS_NOT_FOUND));
        }
        GoalAnalysisSnapshot snapshot = liveAnalysis.get();
        if (!userId.equals(snapshot.userId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }

        if (snapshot.normalizedGoal() == null || snapshot.normalizedGoal().isBlank()) {
            throw new BusinessException(ChallengeErrorCode.ANALYSIS_STALE);
        }

        // 재사용할 작품이 있는지 먼저 확인하고, 없을 때만 대표 작업을 찾는 데 필요한 Redis 키를 만드는 코드
        Optional<Long> reusableArtworkId = reusableArtworkService.chooseArtwork(snapshot);
        String categoryKey = reusableArtworkId.isEmpty() ? generationKey(snapshot) : null;
        
        Optional<String> lockToken = goalAnalysisStore.acquireConfirmation(
                analysisId, snapshot, request.startDate(), request.endDate(), acceptedAt);
        if (lockToken.isEmpty()) {
            // 다른 요청이 먼저 접수됐는지 다시 확인함.
            var existing = findReceivedChallenge(analysisId, userId, request);
            if (existing.isPresent()) {
                return existing.get();
            }
            if (goalAnalysisStore.findByAnalysisId(analysisId).isEmpty()) {
                throw new BusinessException(ChallengeErrorCode.ANALYSIS_NOT_FOUND);
            }
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE);
        }

        if (reusableArtworkId.isEmpty()) {
            return acceptArtworkTask(analysisId, lockToken.get(), categoryKey, acceptedAt);
        }

        Instant expiresAt = GoalAnalysisStore.generationDeadline(acceptedAt);
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                var response = confirmationTransaction(expiresAt).execute(status -> {
                    try {
                        var saved = confirmCustomChallenge(userId, analysisId, lockToken.get(), request, snapshot,
                                durationDays, reusableArtworkId.get());
                        checkBeforeCommit(expiresAt);
                        return saved;
                    } catch (DataIntegrityViolationException | TransientDataAccessException exception) {
                        throw new CustomChallengeWriteConflict(exception);
                    }
                });
                try {
                    goalAnalysisStore.finishConfirmation(analysisId, lockToken.get());
                } catch (DataAccessException exception) {
                    log.warn("Failed to set retention for committed custom challenge {}", analysisId, exception);
                }
                return response;
            } catch (CustomChallengeWriteConflict exception) {
                if (attempt == 3) {
                    throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE, exception.getCause());
                }
            } catch (TransactionException | DataAccessException exception) {
                throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE, exception);
            }
        }
        throw new IllegalStateException("자유목표 도전을 확정하지 못했습니다.");
    }

    // S3 검증을 마친 대표 작업으로 실제 도전을 만드는 메서드
    public void confirmGeneratedArtwork(ClaimedTask work) {
        var receipt = goalAnalysisStore.findCustomReceipt(work.analysisId())
                .orElseThrow(() -> new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE));
        var request = new CustomChallengeCreateRequest(work.analysisId(), receipt.startDate(), receipt.endDate());
        var snapshot = artworkWorkStore.readAnalysis(work);
        var result = artworkWorkStore.readResult(work);
        Long artworkId = confirmationTransaction(work.expiresAt()).execute(status ->
                saveGeneratedChallenge(work, receipt.userId(), request, snapshot, result));
        artworkWorkStore.markSucceeded(work, artworkId);
    }

    // confirmGeneratedArtwork()가 시작한 DB 트랜잭션 안에서 미디어, 작품, 도전을 실제로 저장하는 내부 메서드
    private Long saveGeneratedChallenge(ClaimedTask work, Long userId, CustomChallengeCreateRequest request,
            GoalAnalysisSnapshot snapshot, ArtworkGenerationResult result) {
        // 같은 작업의 이전 저장이 끝날 때까지 기다린 뒤 DB 상태를 다시 확인.
        AiTask task = aiTaskRepository.findByAiTaskIdAndTaskType(work.taskId(), AiTaskType.GENERATE_ARTWORK)
                .orElseThrow(() -> new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE));
        requireTime(work.expiresAt());
        if (!work.verified() || task.getTaskVersion() != work.taskVersion()
                || result == null || result.status() != ArtworkGenerationResult.Status.SUCCEEDED
                || !Objects.equals(result.taskId(), work.taskId()) || !Objects.equals(result.taskVersion(), work.taskVersion())
                || !userId.equals(snapshot.userId())) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE);
        }
        if (task.getStatus() == AiTaskStatus.SUCCEEDED && task.getArtworkId() != null) {
            return task.getArtworkId();
        }
        if (task.getStatus() != AiTaskStatus.PENDING && task.getStatus() != AiTaskStatus.RUNNING) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE);
        }

        var image = result.image();
        var source = result.source();
        var media = mediaRepository.save(Media.createGeneratedArtwork(
                image.key(), image.sha256(), image.contentType(), image.sizeBytes()));
        String title = source.title();
        int titleLength = Math.min(100, title.codePointCount(0, title.length()));
        Artwork artwork = artworkRepository.save(Artwork.createGenerated(media.getMediaId(),
                title.substring(0, title.offsetByCodePoints(0, titleLength)), work.taskVersion(),
                LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
        int durationDays = calculateDurationDays(request.startDate(), request.endDate());
        Long challengeId = saveCustomChallenge(userId, request, snapshot, durationDays, artwork.getArtworkId());
        task.completeArtworkGeneration(artwork.getArtworkId());
        challengeRepository.flush();
        artworkMetadataStore.save(artwork.getArtworkId(), new GeneratedArtworkMetadata(source.workId(), source.title(),
                URI.create(source.sourceUrl()), source.rightsCode(), URI.create(source.rightsUrl()),
                source.checkedAt(), result.reuseAllowed()));
        if (!artworkWorkStore.beforeCommit(work, new GoalConfirmationSnapshot(userId, challengeId))) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE);
        }
        checkBeforeCommit(work.expiresAt());
        return artwork.getArtworkId();
    }

    // 도전 확정에 사용할 DB 트랜잭션의 설정을 만드는 메서드
    private TransactionTemplate confirmationTransaction(Instant expiresAt) {
        requireTime(expiresAt);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        transaction.setTimeout(Math.max(1, (int) Duration.between(Instant.now(), expiresAt).toSeconds()));
        return transaction;
    }

    // 호출한 순간에 기한이 남았는지 확인
    // 120초 기한을 넘긴 도전 확정을 막기 위해 사용
    private void requireTime(Instant expiresAt) {
        if (!Instant.now().isBefore(expiresAt)) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE);
        }
    }

    // requireTime()으로 지금 한 번 확인하고 Spring이 DB 커밋을 시작하기 직전에 한 번 더 확인
    // 120초 기한을 넘긴 도전 확정을 막기 위해 사용
    private void checkBeforeCommit(Instant expiresAt) {
        requireTime(expiresAt);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 저장을 끝내기 직전에 기한을 한 번 더 확인.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    requireTime(expiresAt);
                }
            });
        }
    }

    private CustomChallengeCreateResponse acceptArtworkTask(
            String analysisId, String lockToken, String categoryKey, Instant acceptedAt) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        AiTask task;
        try {
            task = transaction.execute(status -> aiTaskRepository.save(AiTask.pendingArtworkGeneration(
                    LocalDateTime.ofInstant(acceptedAt, ZoneId.of("Asia/Seoul")))));
        } catch (TransactionException | DataAccessException exception) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE, exception);
        }

        // DB에 저장된 작업 번호로만 대표 작업을 정한다.
        var pending = goalAnalysisStore.registerArtworkTask(
                analysisId, lockToken, categoryKey, task.getAiTaskId(), task.getTaskVersion(), acceptedAt);
        return pendingResponse(pending);
    }

    private String generationKey(GoalAnalysisSnapshot snapshot) {
        CategoryPath path = snapshot.categoryPath();
        var activeId = goalCategoryRepository.findActiveMinorId(
                path.major().strip(), path.middle().strip(), path.minor().strip());
        if (snapshot.categoryId() != null && !snapshot.categoryId().equals(activeId.orElse(null))) {
            throw new BusinessException(ChallengeErrorCode.ANALYSIS_STALE);
        }
        if (activeId.isPresent()) {
            // 이미 등록된 분류는 DB에 저장된 이름을 기준으로 같은 경로를 만든다.
            var names = goalCategoryRepository.findCategoryNames(activeId.get())
                    .orElseThrow(() -> new BusinessException(ChallengeErrorCode.ANALYSIS_STALE));
            path = new CategoryPath(names.getMajorName(), names.getMiddleName(), names.getMinorName());
        }
        return categoryKeyPart(path.major()) + categoryKeyPart(path.middle()) + categoryKeyPart(path.minor());
    }

    private String categoryKeyPart(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKC).strip().toLowerCase(Locale.ROOT);
        return normalized.length() + ":" + normalized;
    }

    private CustomChallengeCreateResponse pendingResponse(GoalAnalysisStore.PendingArtworkTask task) {
        return CustomChallengeCreateResponse.pending(
                task.taskId(), task.taskVersion(), task.acceptedAt(), task.expiresAt());
    }

    // 저장 중 충돌이 나고 DB 변경이 취소된 경우에만 다시 시도.
    private static final class CustomChallengeWriteConflict extends RuntimeException {
        private CustomChallengeWriteConflict(DataAccessException cause) {
            super(cause);
        }
    }

    private Optional<CustomChallengeCreateResponse> findReceivedChallenge(
            String analysisId, Long userId, CustomChallengeCreateRequest request) {
        var receipt = goalAnalysisStore.findCustomReceipt(analysisId);
        receipt.ifPresent(existing -> {
            if (!userId.equals(existing.userId())) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN);
            }
            if (!request.startDate().equals(existing.startDate()) || !request.endDate().equals(existing.endDate())) {
                throw new BusinessException(ChallengeErrorCode.ANALYSIS_ALREADY_CONFIRMED);
            }
        });
        var confirmed = findConfirmation(analysisId, userId, request, false);
        if (confirmed.isEmpty() && receipt.isPresent()) {
            var task = goalAnalysisStore.findArtworkTask(analysisId);
            if (task.isPresent()) {
                return task.map(this::pendingResponse);
            }
        }
        // 기존 도전이 저장됐는지 알 수 없으면 새 도전을 만들지 않음.
        if (receipt.isPresent() && confirmed.isEmpty()) {
            throw new BusinessException(ChallengeErrorCode.ARTWORK_TASK_UNAVAILABLE);
        }
        return confirmed;
    }

    // Redis에 기록된 도전이 실제로 저장됐는지 확인
    private Optional<CustomChallengeCreateResponse> findConfirmation(
            String analysisId, Long userId, CustomChallengeCreateRequest request, boolean lock) {
        Optional<GoalConfirmationSnapshot> result = goalAnalysisStore.findConfirmation(analysisId);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        GoalConfirmationSnapshot confirmation = result.get();
        if (!userId.equals(confirmation.userId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        Optional<Challenge> challenge = lock
                ? challengeRepository.findForConfirmation(confirmation.challengeId())
                : challengeRepository.findById(confirmation.challengeId());
        return challenge.map(existing -> confirmedResponse(existing, userId, request));
    }

    // 소유자와 요청 내용 확인 후 기존 도전 ID 반환
    private CustomChallengeCreateResponse confirmedResponse(
            Challenge challenge, Long userId, CustomChallengeCreateRequest request) {
        if (!userId.equals(challenge.getUserId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (!challenge.getStartDate().equals(request.startDate())
                || !challenge.getEndDate().equals(request.endDate())) {
            throw new BusinessException(ChallengeErrorCode.ANALYSIS_ALREADY_CONFIRMED);
        }
        return new CustomChallengeCreateResponse(challenge.getChallengeId());
    }

    // 자유목표 도전 생성
    private CustomChallengeCreateResponse confirmCustomChallenge(
            Long userId, String analysisId, String lockToken, CustomChallengeCreateRequest request,
            GoalAnalysisSnapshot snapshot, int durationDays, Long artworkId) {
        Optional<CustomChallengeCreateResponse> confirmed = findConfirmation(analysisId, userId, request, true);
        if (confirmed.isPresent()) {
            return confirmed.get();
        }

        Long challengeId = saveCustomChallenge(userId, request, snapshot, durationDays, artworkId);
        // 응답이 끊겨도 다음 요청에서 도전을 찾을 수 있게 ID를 먼저 기록한다.
        challengeRepository.flush();
        goalAnalysisStore.saveConfirmation(analysisId, lockToken, new GoalConfirmationSnapshot(userId, challengeId));
        return new CustomChallengeCreateResponse(challengeId);
    }

    private Long saveCustomChallenge(Long userId, CustomChallengeCreateRequest request, GoalAnalysisSnapshot snapshot,
            int durationDays, Long artworkId) {
        CategoryPath path = snapshot.categoryPath();
        // 분석 때 사용한 분류가 아직 유효한지 확인
        if (snapshot.categoryId() != null) {
            Long activeId = goalCategoryRepository.findActiveMinorId(
                    path.major().strip(), path.middle().strip(), path.minor().strip())
                    .orElseThrow(() -> new BusinessException(ChallengeErrorCode.ANALYSIS_STALE));
            if (!activeId.equals(snapshot.categoryId())) {
                throw new BusinessException(ChallengeErrorCode.ANALYSIS_STALE);
            }
        }

        GoalCategory major = resolveCategory(null, path.major(), snapshot.categoryId() != null);
        GoalCategory middle = resolveCategory(major.getCateId(), path.middle(), snapshot.categoryId() != null);
        GoalCategory minor = resolveCategory(middle.getCateId(), path.minor(), snapshot.categoryId() != null);
        if (snapshot.categoryId() != null && !snapshot.categoryId().equals(minor.getCateId())) {
            throw new BusinessException(ChallengeErrorCode.ANALYSIS_STALE);
        }
        GoalTemplate template = resolveCustomTemplate(minor.getCateId(), snapshot);

        // 기본 목표로도 선택할 수 있도록 작품 연결
        linkArtwork(template.getGoalTemplateId(), artworkId);

        int[] gridSize = calculateGridSize(durationDays);
        Challenge challenge = Challenge.create(
                userId,
                minor.getCateId(),
                template.getGoalTemplateId(),
                artworkId,
                snapshot.normalizedGoal(),
                snapshot.normalizedGoal(),
                snapshot.goal(),
                snapshot.photoGuide(),
                durationDays,
                request.startDate(),
                request.endDate(),
                snapshot.verificationScope(),
                gridSize[0],
                gridSize[1]);
        challengeRepository.save(challenge);

        List<ChallengeCondition> conditions = new ArrayList<>();
        for (int index = 0; index < snapshot.conditions().size(); index++) {
            conditions.add(ChallengeCondition.create(
                    challenge.getChallengeId(), "C" + (index + 1), snapshot.conditions().get(index)));
        }
        challengeConditionRepository.saveAll(conditions);
        // 도전이 만들어지기 전에 마감된 회차는 실패로 기록한다.
        createSchedules(challenge.getChallengeId(), request.startDate(), durationDays,
                LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        return challenge.getChallengeId();
    }

    private void linkArtwork(Long templateId, Long artworkId) {
        var link = goalTemplateArtworkRepository.findByGoalTemplateIdAndArtworkId(templateId, artworkId);
        if (link.isEmpty()) {
            goalTemplateArtworkRepository.save(GoalTemplateArtwork.create(templateId, artworkId));
        } else if (!"N".equals(link.get().getDelYn())) {
            goalTemplateArtworkRepository.restore(link.get().getGoalTemplateArtworkId());
        }
    }

    // 기존 분류 재사용, 삭제된 분류 복구
    private GoalCategory resolveCategory(Long parentId, String name, boolean requireActive) {
        Optional<GoalCategory> existing = goalCategoryRepository.findForConfirmation(parentId, name.strip());
        if (existing.isPresent()) {
            GoalCategory category = existing.get();
            if (requireActive && !"N".equals(category.getDelYn())) {
                throw new BusinessException(ChallengeErrorCode.ANALYSIS_STALE);
            }
            if (!"N".equals(category.getDelYn())) {
                goalCategoryRepository.restore(category.getCateId());
                return goalCategoryRepository.findById(category.getCateId())
                        .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
            }
            return category;
        }
        if (requireActive) {
            throw new BusinessException(ChallengeErrorCode.ANALYSIS_STALE);
        }
        return goalCategoryRepository.saveAndFlush(GoalCategory.create(
                parentId, name.strip(), "AI_" + UUID.randomUUID().toString().replace("-", "")));
    }

    // 목표와 인증 정보가 같으면 기존 템플릿 재사용
    private GoalTemplate resolveCustomTemplate(Long categoryId, GoalAnalysisSnapshot snapshot) {
        List<GoalTemplate> templates = goalTemplateRepository.findReusableTemplates(
                categoryId, snapshot.normalizedGoal(), snapshot.photoGuide(), snapshot.verificationScope());
        for (GoalTemplate template : templates) {
            List<GoalTemplateCondition> conditions = goalTemplateConditionRepository
                    .findByGoalTemplateIdAndDelYn(template.getGoalTemplateId(), "N");
            boolean matches = conditions.size() == snapshot.conditions().size();
            for (int index = 0; matches && index < snapshot.conditions().size(); index++) {
                String code = "C" + (index + 1);
                String description = snapshot.conditions().get(index);
                matches = conditions.stream().anyMatch(condition -> code.equals(condition.getCode())
                        && description.equals(condition.getDescription()));
            }
            if (matches) {
                return template;
            }
        }

        GoalTemplate template = goalTemplateRepository.save(GoalTemplate.create(
                categoryId, snapshot.normalizedGoal(), snapshot.photoGuide(), snapshot.verificationScope()));
        List<GoalTemplateCondition> conditions = new ArrayList<>();
        for (int index = 0; index < snapshot.conditions().size(); index++) {
            conditions.add(GoalTemplateCondition.create(
                    template.getGoalTemplateId(), "C" + (index + 1), snapshot.conditions().get(index)));
        }
        goalTemplateConditionRepository.saveAll(conditions);
        return template;
    }
}
