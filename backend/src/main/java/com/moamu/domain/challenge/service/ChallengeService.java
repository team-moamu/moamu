package com.moamu.domain.challenge.service;

import com.moamu.domain.schedule.repository.ScheduleRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
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
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
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
