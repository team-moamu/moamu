package com.moamu.domain.artwork.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.moamu.domain.artwork.client.ArtworkGenerationClient;
import com.moamu.domain.artwork.client.ArtworkGenerationClientException;
import com.moamu.domain.artwork.dto.ArtworkGenerationRequest;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore.ClaimedTask;

@Service
public class ArtworkGenerationWorker {

    private final ArtworkGenerationWorkStore workStore;
    private final ArtworkGenerationClient client;
    private final Duration lease;

    public ArtworkGenerationWorker(ArtworkGenerationWorkStore workStore, ArtworkGenerationClient client,
            @Value("${app.ai.connect-timeout}") Duration connectTimeout,
            @Value("${app.ai.read-timeout}") Duration readTimeout) {
        this.workStore = workStore;
        this.client = client;
        this.lease = connectTimeout.plus(readTimeout).plusSeconds(5);
    }

    public void runNext() {
        workStore.claimNext(Instant.now(), lease).ifPresent(this::process);
    }

    // 본문이 이미 있으면 먼저 AI 서버에 같은 작업이 접수됐는지 확인.
    private void process(ClaimedTask task) {
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
