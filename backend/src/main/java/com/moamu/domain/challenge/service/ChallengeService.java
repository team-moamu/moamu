package com.moamu.domain.challenge.service;

import com.moamu.domain.schedule.repository.ScheduleRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.artwork.entity.Artwork;
import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
import com.moamu.domain.challenge.dto.ChallengeCompletionResponse;
import com.moamu.domain.challenge.dto.ChallengeSettlementResponse;
import com.moamu.domain.challenge.enums.Status;
import com.moamu.domain.challenge.exception.ChallengeErrorCode;
import com.moamu.domain.challenge.dto.TodayChallengeResponse;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.challenge.entity.ChallengeCondition;
import com.moamu.domain.challenge.repository.ChallengeConditionRepository;
import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.goal.entity.GoalTemplate;
import com.moamu.domain.goal.entity.GoalTemplateArtwork;
import com.moamu.domain.goal.entity.GoalTemplateCondition;
import com.moamu.domain.goal.repository.GoalTemplateArtworkRepository;
import com.moamu.domain.goal.repository.GoalTemplateConditionRepository;
import com.moamu.domain.goal.repository.GoalTemplateRepository;
import com.moamu.domain.schedule.entity.Schedule;
import com.moamu.domain.schedule.service.ScheduleService;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.verification.service.ArtworkImageUrlService;
import com.moamu.domain.verification.enums.VerificationStatus;
import com.moamu.domain.verification.repository.VerificationRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import lombok.RequiredArgsConstructor;

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
                case FAILED -> { }
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
                VerificationStatus.REVIEW_REQUIRED, VerificationStatus.TECH_ERROR)
        );
        if (pendingVerificationCount > 0) {
            throw new BusinessException(ChallengeErrorCode.CHALLENGE_NOT_COMPLETED);
        }
        revealedPieceIndices.sort(Integer::compareTo);

        // 도전 생성 당시 연결된 작품과 기존 S3 서명 URL 생성 기능 재사용
        Artwork artwork = artworkRepository.findByArtworkIdAndDelYn(challenge.getArtworkId(), "N")
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        String imageKey = artwork.getMediaId() == null ? null : mediaRepository.findById(artwork.getMediaId())
            .map(media -> media.getImageKey()).orElse(null);
        ChallengeCompletionResponse.Artwork artworkResponse = new ChallengeCompletionResponse.Artwork(
            artwork.getArtworkId(), artwork.getArtworkTitle(), artworkImageUrlService.resolve(imageKey)
        );

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
            earnedPieceCount == totalPieceCount
        );
    }

    @Transactional
    public void createChallenge(Long userId, ChallengeCreateRequest request) {

        // 사용자 조회 -> 회원 기능 완료되면 적용
        // User user = userRepository.findById(userId)
        //     .orElseThrow(() ->
        //         new IllegalArgumentException("존재하지 않는 회원입니다.")
        // );

        // 목표 템플릿 조회
        GoalTemplate goalTemplate = goalTemplateRepository
            .findByGoalTemplateIdAndDelYn(request.goalTemplateId(), "N")
            .orElseThrow(() ->
                new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
        );

        // 도전 기간 검증 (7, 14, 30일)
        int durationDays = request.durationDays();

        validateDurationDays(durationDays);

        LocalDate startDate = request.startDate();
        LocalDate endDate = startDate.plusDays(durationDays - 1);

        // 선택한 목표에 연결된 작품 조회
        GoalTemplateArtwork goalTemplateArtwork = goalTemplateArtworkRepository
            .findByGoalTemplateIdAndDelYn(request.goalTemplateId(), "N")
            .orElseThrow(() ->
                new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
            );

        // 연결된 작품 ID 가져오기
        Long artworkId = goalTemplateArtwork.getArtworkId();

        // 작품 존재 여부 확인
        artworkRepository.findByArtworkIdAndDelYn(artworkId, "N")
            .orElseThrow(() ->
                new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
            );

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
            null,   // 1차 mvp 사용자 입력 도전 제외
            goalTemplate.getPhotoGuide(),
            durationDays,
            startDate,
            endDate,
            goalTemplate.getVerificationScope(),
            gridSize[0],
            gridSize[1]
        );

        // Challenge 저장
        challengeRepository.save(challenge);
        
        // 목표 템플릿 인증 조건 조회
        List<GoalTemplateCondition> conditions = goalTemplateConditionRepository.findByGoalTemplateIdAndDelYn(request.goalTemplateId(), "N");


        List<ChallengeCondition> challengeConditions = conditions.stream()
            .map(condition -> ChallengeCondition.create(
                challenge.getChallengeId(),
                condition.getCode(),
                condition.getDescription()
            ))
            .toList();
        challengeConditionRepository.saveAll(challengeConditions);


        // 도전 기간에 맞춰 Schedule 생성
        List<Schedule> Schedules = new ArrayList<>();

        for (int i = 0; i < durationDays; i++) {
            LocalDate scheduleDate = startDate.plusDays(i);

            Schedule schedule = Schedule.create(
                challenge.getChallengeId(),
                i + 1,
                scheduleDate,
                scheduleDate.atStartOfDay(),
                scheduleDate.plusDays(1).atStartOfDay(),
                i
            );

            Schedules.add(schedule);
        }

        // Schedule 저장
        scheduleRepository.saveAll(Schedules);
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
                default -> { }
            }
        }
        int pendingCount = schedules.size() - successCount - failedCount;

        // 기술 오류 재처리 정책은 미정이므로 최신 TECH_ERROR도 완료 대기 대상으로 유지
        long pendingVerificationCount = verificationRepository.countPendingLatestVerifications(
            challengeId,
            List.of(VerificationStatus.SUBMITTED, VerificationStatus.ANALYZING,
                VerificationStatus.REVIEW_REQUIRED, VerificationStatus.TECH_ERROR)
        );
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
            allFinalized
        );
    }

    // 종료 처리 전 도전 기간과 회차의 개수·날짜·순번 검증
    private void validateSchedules(Challenge challenge, List<Schedule> schedules) {
        if (schedules.size() != challenge.getDurationDays()
                || !challenge.getStartDate().plusDays(challenge.getDurationDays() - 1L).equals(challenge.getEndDate())) {
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

    // 도전 기간 검증 메서드
    private void validateDurationDays(int durationDays) {
        if (durationDays != 7 && durationDays != 14 &&  durationDays != 30) {
        // if (durationDays < 1) {
            throw new BusinessException (CommonErrorCode.INVALID_REQUEST);
        }
    }

    // 퍼즐 그리드 계산
    private int[] calculateGridSize(int durationDays) {
        int cols = (int) Math.ceil(Math.sqrt(durationDays));
        int rows = (int) Math.ceil((double) durationDays / cols);

        return new int[]{rows, cols};
    }

    // 오늘의 도전 조회
    public List<TodayChallengeResponse> getTodayChallenges(Long userId, LocalDate day, String sort) {
        // 사용자 조회 -> 회원 기능 완료되면 적용
        // User user = userRepository.findById(userId)
        //     .orElseThrow(() ->
        //         new IllegalArgumentException("존재하지 않는 회원입니다.")
        // );

        // 파라미터로 받는 날짜 없으면 오늘 날짜로 처리
        LocalDate targetDate = day != null ? day : LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 정렬 기준
        // 최신순 - 기본값(내림차순) / 이름순(오름차순)
        String sortType = "name".equals(sort) ? "name" : "latest";

        return challengeRepository.findTodayChallenges(userId, targetDate, sortType);
    }
}
