package com.moamu.domain.artwork.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.moamu.domain.artwork.client.ArtworkGenerationClient;
import com.moamu.domain.artwork.client.ArtworkGenerationClientException;
import com.moamu.domain.artwork.dto.ArtworkGenerationRequest;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore.ClaimedTask;

@Service
public class ArtworkGenerationWorker {

    private final ArtworkGenerationWorkStore workStore;
    private final ArtworkGenerationClient client;
    private final GeneratedArtworkValidator validator;
    private final ChallengeService challengeService;
    private final Duration lease;

    public ArtworkGenerationWorker(ArtworkGenerationWorkStore workStore, ArtworkGenerationClient client,
            GeneratedArtworkValidator validator, ChallengeService challengeService,
            @Value("${app.ai.connect-timeout}") Duration connectTimeout,
            @Value("${app.ai.read-timeout}") Duration readTimeout) {
        this.workStore = workStore;
        this.client = client;
        this.validator = validator;
        this.challengeService = challengeService;
        this.lease = connectTimeout.plus(readTimeout).plusSeconds(5);
    }

    public void runNext() {
        workStore.claimNext(Instant.now(), lease).ifPresent(this::process);
    }

    private void process(ClaimedTask task) {
        if (task.verified()) {
            challengeService.confirmGeneratedArtwork(task);
            return;
        }
        // 저장된 생성 결과가 있으면 FastAPI를 다시 부르지 않고 S3를 확인.
        if (task.resultJson() != null) {
            validate(task);
            return;
        }
        boolean submit = task.requestBody() == null || task.submitAgain();
        try {
            if (submit) {
                submit(task);
                return;
            }
            var result = client.getStatus(task.taskId(), task.taskVersion());
            switch (result.status()) {
                case PENDING, RUNNING -> workStore.pollAgain(task);
                case FAILED -> workStore.fail(task, result.errorCode(), result.retryable());
                case SUCCEEDED -> workStore.saveResult(task, result);
            }
        } catch (ArtworkGenerationClientException exception) {
            switch (exception.getReason()) {
                case TASK_NOT_FOUND -> workStore.submitAgain(task);
                case PAYLOAD_CONFLICT -> workStore.fail(task, "ARTWORK_GENERATION_PAYLOAD_CONFLICT", false);
                default -> workStore.pollAgain(task);
            }
        }
    }

    private void validate(ClaimedTask task) {
        try {
            validator.validate(task.taskId(), task.taskVersion(), task.expiresAt(), workStore.readResult(task));
            workStore.markVerified(task);
        } catch (GeneratedArtworkValidator.ValidationException exception) {
            workStore.fail(task, "ARTWORK_VALIDATION_FAILED", exception.isRetryable());
        }
    }

    private void submit(ClaimedTask task) {
        String body = task.requestBody();
        if (body == null) {
            var analysis = workStore.readAnalysis(task);
            body = client.serializeRequest(new ArtworkGenerationRequest(task.taskId(), task.taskVersion(),
                    task.expiresAt(), analysis.normalizedGoal(), analysis.categoryPath(), analysis.conditions(),
                    analysis.photoGuide()));
        }
        // AI 서버를 부르기 전에 본문을 남겨 두면 응답이 끊겨도 그대로 다시 보낼 수 있음.
        if (workStore.beforeSubmit(task, body)) {
            client.submit(body);
            workStore.pollAgain(task);
        }
    }
}
