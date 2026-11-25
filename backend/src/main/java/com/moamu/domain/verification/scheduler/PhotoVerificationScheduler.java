package com.moamu.domain.verification.scheduler;

import com.moamu.domain.verification.service.PhotoVerificationWorker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import lombok.RequiredArgsConstructor;

@Configuration
@Profile({"local", "prod"})
@ConditionalOnProperty(name="app.photo-verification.worker-enabled", havingValue="true", matchIfMissing=true)
@EnableScheduling
@RequiredArgsConstructor
public class PhotoVerificationScheduler {
    private final PhotoVerificationWorker worker;

    // 사진 인증 대기가 작품 생성이나 회차 마감을 막지 않도록 실행 스레드를 분리
    @Bean(defaultCandidate=false)
    ThreadPoolTaskScheduler photoVerificationTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("photo-verification-");
        return scheduler;
    }

    @Scheduled(fixedDelayString="${app.photo-verification.worker-delay-ms:1000}", scheduler="photoVerificationTaskScheduler")
    public void process() { worker.runNext(); }
}
