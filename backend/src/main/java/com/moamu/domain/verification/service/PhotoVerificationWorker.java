package com.moamu.domain.verification.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.dao.TransientDataAccessException;
import com.moamu.domain.verification.repository.PhotoVerificationRequestRepository;
import com.moamu.domain.challenge.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoVerificationWorker {
    private final PhotoVerificationWorkStore workStore;
    private final PhotoVerificationRequestRepository requests;
    private final PhotoVerificationService verifier;
    private final ChallengeService challenges;

    // 한 번에 하나만 처리
    public void runNext() {
        var claimed = workStore.claimNext();
        if (claimed.isEmpty()) return;
        var task = claimed.get();
        if (task.retryCount() > 2) { workStore.fail(task, false); return; }
        boolean completed;
        try {
            var request = requests.findByAiTaskIdAndUserId(task.taskId(), task.userId()).orElseThrow();
            var result = verifier.verify(request);
            completed = workStore.complete(task, request, result);
        } catch (RuntimeException error) {
            log.warn("사진 인증 작업 {} 처리 실패: {}", task.taskId(), error.getClass().getSimpleName());
            workStore.fail(task, retryable(error));
            return;
        }
        if (completed) challenges.completeAfterVerification(task.userId(), task.verificationId());
    }

    // 오류의 원인을 확인해 다시 시도할 수 있는지 판단
    private boolean retryable(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ResourceAccessException || cause instanceof TransientDataAccessException) return true;
            if (cause instanceof RestClientResponseException response &&
                (response.getStatusCode().is5xxServerError() || response.getStatusCode().value() == 429)) return true;
        }
        return false;
    }
}
