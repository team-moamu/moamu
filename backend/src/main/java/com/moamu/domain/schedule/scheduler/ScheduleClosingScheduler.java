package com.moamu.domain.schedule.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.challenge.enums.Status;
import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.domain.schedule.service.ScheduleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 웹 요청 없이 지난 회차를 마감. 서버 재시작 후에도 누락된 과거 회차를 다시 확인
@Configuration
@Profile({"local", "prod"})
@ConditionalOnProperty(name = "app.schedule-closing.enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduleClosingScheduler {

    private final ChallengeRepository challengeRepository;
    private final ScheduleService scheduleService;
    private final ChallengeService challengeService;

    // 완료 후 1분 간격으로 실행. 각 도전은 서비스의 별도 트랜잭션에서 처리
    @Scheduled(fixedDelayString = "${app.schedule-closing.delay-ms:60000}",
        initialDelayString = "${app.schedule-closing.delay-ms:60000}")
    public void closeExpiredSchedules() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        long afterId = 0;
        while (true) {
            List<Challenge> candidates = challengeRepository.findClosingCandidates(
                today, afterId, List.of(Status.IN_PROGRESS, Status.SETTLING), PageRequest.of(0, 100)
            );
            if (candidates.isEmpty()) {
                return;
            }
            for (Challenge challenge : candidates) {
                try {
                    if (challenge.getEndDate().isBefore(today)) {
                        // 전체 기간이 끝난 도전은 회차 마감과 도전 완료를 함께 처리
                        challengeService.settle(challenge.getUserId(), challenge.getChallengeId());
                    } else {
                        scheduleService.closeExpiredSchedules(challenge.getUserId(), challenge.getChallengeId());
                    }
                } catch (RuntimeException exception) {
                    // 한 도전의 오류가 다른 도전의 마감을 막지 않도록 다음 도전 계속 처리
                    log.warn("도전 {}의 자동 마감에 실패했습니다. 다음 실행에서 재시도합니다.",
                        challenge.getChallengeId(), exception);
                }
            }
            afterId = candidates.getLast().getChallengeId();
        }
    }
}
