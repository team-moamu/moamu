package com.moamu.domain.verification;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.domain.verification.dto.ChallengeVerificationResponseDto;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class VerificationService {
  private final ScheduleRepository scheduleRepository;

  public ChallengeVerificationResponseDto getVerification(Long challengeId, Long scheduleId) {

    // challengeId, scheduleId로 도전 정보 및 인증 조건 조회
    List<ChallengeVerificationResponseDto> rows = scheduleRepository.findByChallengeIdAndScheduleId(challengeId, scheduleId);

    // 없으면 에러 처리 
    if (rows.isEmpty()) {
      throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    // 도전 정보
    ChallengeVerificationResponseDto first = rows.getFirst();
    // 인증 조건
    List<String> descriptions = rows.stream()
        .flatMap(row -> row.description().stream())
        .toList();

    return new ChallengeVerificationResponseDto(
        first.challengeId(), first.challengeTitle(), first.scheduleId(),
        first.dayNo(), first.dayCount(), first.attemptCount(), descriptions);
  }
}
