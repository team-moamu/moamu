package com.moamu.domain.schedule.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.schedule.dto.ScheduleDetailResponse;
import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ChallengeRepository challengeRepository;

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
