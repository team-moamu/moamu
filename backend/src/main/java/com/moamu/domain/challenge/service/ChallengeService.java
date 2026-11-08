package com.moamu.domain.challenge.service;

import com.moamu.domain.schedule.repository.ScheduleRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
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

    // 종료일 당일까지 인증 허용. 한국 시간으로 다음 날이 된 이후에만 종료 처리
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChallengeSettlementResponse settle(Long userId, Long challengeId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (challengeId == null || challengeId <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        // 소유권 및 삭제 여부 확인 후 도전 잠금
        Challenge challenge = challengeRepository.findOwnedForSettlement(userId, challengeId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if (!challenge.getEndDate().isBefore(LocalDate.now(ZoneId.of("Asia/Seoul")))) {
            throw new BusinessException(ChallengeErrorCode.CHALLENGE_NOT_ENDED);
        }
        if (challenge.getStatus() != Status.IN_PROGRESS && challenge.getStatus() != Status.SETTLING
                && challenge.getStatus() != Status.COMPLETED) {
            throw new BusinessException(ChallengeErrorCode.CHALLENGE_SETTLEMENT_NOT_ALLOWED);
        }

        // 누락되거나 기간과 맞지 않는 회차로 도전이 완료되는 것을 방지
        List<Schedule> schedules = scheduleRepository.findSchedulesForSettlement(challengeId);
        validateSchedules(challenge, schedules);

        // 회차 상태는 읽기만 하며 기한 경과에 따른 실패 확정은 회차 담당 로직에서 수행
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
        } else {
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
