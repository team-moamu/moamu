package com.moamu.domain.artwork.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.moamu.domain.artwork.service.ArtworkGenerationWorker;

import lombok.RequiredArgsConstructor;

@Configuration
@Profile({ "local", "prod" })
@ConditionalOnProperty(name = "app.artwork-generation.worker-enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
@RequiredArgsConstructor
public class ArtworkGenerationScheduler {

    private final ArtworkGenerationWorker worker;

    // AI 응답을 기다리는 동안 회차 마감 작업이 함께 기다리지 않도록 함.
    @Bean(defaultCandidate = false)
    ThreadPoolTaskScheduler artworkGenerationTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("artwork-generation-");
        return scheduler;
    }

    @Scheduled(fixedDelayString = "${app.artwork-generation.worker-delay-ms:1000}", scheduler = "artworkGenerationTaskScheduler")
    public void processArtworkGeneration() {
        worker.runNext();
    }
}
