package com.moamu.domain.schedule.service;

import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.schedule.dto.ScheduleDetailResponse;
import com.moamu.domain.schedule.entity.Schedule;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.challenge.enums.Status;
import com.moamu.domain.verification.enums.VerificationStatus;
import com.moamu.domain.verification.repository.VerificationRepository;
import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ChallengeRepository challengeRepository;
    private final VerificationRepository verificationRepository;

    // 도전 종료 처리와 동일하게 도전 → 회차 순서로 잠금. 반복 실행해도 확정 회차는 변경하지 않음
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int closeExpiredSchedules(Long userId, Long challengeId) {
        Challenge challenge = challengeRepository.findOwnedForSettlement(userId, challengeId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        if (challenge.getStatus() != Status.IN_PROGRESS && challenge.getStatus() != Status.SETTLING) {
            return 0;
        }

        List<Schedule> schedules = scheduleRepository.findSchedulesForSettlement(challengeId);
        Set<Long> excludedScheduleIds = Set.copyOf(verificationRepository
            .findScheduleIdsExcludedFromClosing(challengeId, VerificationStatus.FAILED));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        int closedCount = 0;
        for (Schedule schedule : schedules) {
            if (!excludedScheduleIds.contains(schedule.getScheduleId()) && schedule.closeIfExpired(now)) {
                closedCount++;
            }
        }
        // @Version 검사를 포함해 변경을 저장. 충돌 시 해당 도전의 마감 트랜잭션 전체 롤백
        scheduleRepository.flush();
        return closedCount;
    }

    public List<ScheduleDetailResponse> getScheduleDetail(Long userId, Long challengeId) {
        // 사용자 조회 -> 회원 기능 완료되면 적용
        // userRepository.findById(userId)
        //     .orElseThrow(() ->
        //         new IllegalArgumentException("존재하지 않는 회원입니다.")
        // );

        // challengeId 조회
        challengeRepository.findById(challengeId)
            .orElseThrow(() ->
                new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
        );

        return scheduleRepository.findByUserIdAndChallengeId(userId, challengeId);
    }
}
