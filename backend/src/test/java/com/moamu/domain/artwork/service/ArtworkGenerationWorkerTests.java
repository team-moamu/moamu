package com.moamu.domain.artwork.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moamu.domain.artwork.client.ArtworkGenerationClient;
import com.moamu.domain.artwork.client.ArtworkGenerationClientException;
import com.moamu.domain.artwork.client.ArtworkGenerationClientException.Reason;
import com.moamu.domain.artwork.dto.ArtworkGenerationRequest;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult.Status;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore.ClaimedTask;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore.RepresentativeOutcome;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore.RepresentativeState;

@ExtendWith(MockitoExtension.class)
class ArtworkGenerationWorkerTests {

    private static final Instant EXPIRES_AT = Instant.parse("2026-11-24T04:02:00Z");
    private static final String BODY = "{\"taskId\":91,\"taskVersion\":1}";
    @Mock private ArtworkGenerationWorkStore store;
    @Mock private ArtworkGenerationClient client;
    @Mock private GeneratedArtworkValidator validator;
    @Mock private ChallengeService challengeService;

    @Test
    void savesTheRequestBeforeSubmitting() {
        var task = claim(null, false);
        var path = new CategoryPath("Health", "Exercise", "Walking");
        var snapshot = new GoalAnalysisSnapshot(1L, "Walk", "Walk daily", null, path,
                List.of("Shoes", "Trail"), "Show the trail", "Outdoor");
        when(store.readAnalysis(task)).thenReturn(snapshot);
        when(client.serializeRequest(new ArtworkGenerationRequest(91L, 1, EXPIRES_AT, "Walk daily", path,
                List.of("Shoes", "Trail"), "Show the trail"))).thenReturn(BODY);
        when(store.beforeSubmit(task, BODY)).thenReturn(true);

        worker().runNext();

        var order = inOrder(store, client);
        order.verify(store).beforeSubmit(task, BODY);
        order.verify(client).submit(BODY);
        order.verify(store).pollAgain(task);
    }

    @Test
    void checksTheExistingVersionAfterRestartAndReusesTheExactBodyIfMissing() {
        var previous = new ClaimedTask("analysis", "token", 91L, 1, EXPIRES_AT, "{}", BODY, false, null, false, 91L, 1, null);
        var again = new ClaimedTask("analysis", "new-token", 91L, 1, EXPIRES_AT, "{}", BODY, true, null, false, 91L, 1, null);
        when(store.claimNext(any(), eq(Duration.ofSeconds(38))))
                .thenReturn(Optional.of(previous), Optional.of(again));
        when(client.getStatus(91L, 1)).thenThrow(new ArtworkGenerationClientException(Reason.TASK_NOT_FOUND, 404, null));
        when(store.beforeSubmit(again, BODY)).thenReturn(true);

        worker().runNext();
        worker().runNext();

        var order = inOrder(store, client);
        order.verify(client).getStatus(91L, 1);
        order.verify(store).submitAgain(previous);
        order.verify(store).beforeSubmit(again, BODY);
        order.verify(client).submit(BODY);
        verify(client, never()).serializeRequest(any());
    }

    @Test
    void skipsTheHttpCallWhenTheLeaseWasLost() {
        claim(BODY, true);

        worker().runNext();

        verifyNoInteractions(client);
    }

    @Test
    void storesSuccessfulHttpResultForValidation() {
        var task = claim(BODY, false);
        var result = new ArtworkGenerationResult(91L, 1, Status.SUCCEEDED, null, null, null, null, true);
        when(client.getStatus(91L, 1)).thenReturn(result);

        worker().runNext();

        verify(store).saveResult(task, result);
        verify(client, never()).submit(any());
    }

    @Test
    void marksTheSavedResultVerifiedWithoutCallingFastApiAgain() {
        var task = claimResult();
        var result = new ArtworkGenerationResult(91L, 1, Status.SUCCEEDED, null, null, null, null, true);
        when(store.readResult(task)).thenReturn(result);

        worker().runNext();

        var order = inOrder(validator, store);
        order.verify(validator).validate(91L, 1, EXPIRES_AT, result);
        order.verify(store).markVerified(task);
        verifyNoInteractions(client);
    }

    @Test
    void doesNotMarkAResultVerifiedWhenS3ValidationFails() {
        var task = claimResult();
        var result = new ArtworkGenerationResult(91L, 1, Status.SUCCEEDED, null, null, null, null, true);
        when(store.readResult(task)).thenReturn(result);
        doThrow(new GeneratedArtworkValidator.ValidationException(false, null))
                .when(validator).validate(91L, 1, EXPIRES_AT, result);

        worker().runNext();

        verify(store).fail(task, "ARTWORK_VALIDATION_FAILED", false);
        verify(store, never()).markVerified(task);
        verifyNoInteractions(client);
    }

    private ClaimedTask claimResult() {
        var task = new ClaimedTask("analysis", "token", 91L, 1, EXPIRES_AT, "{}", BODY, false, "{}", false, 91L, 1, null);
        when(store.claimNext(any(), eq(Duration.ofSeconds(38)))).thenReturn(Optional.of(task));
        return task;
    }

    @ParameterizedTest
    @CsvSource({"FAILED,ARTWORK_REPRESENTATIVE_FAILED", "REUSE_DENIED,ARTWORK_REUSE_NOT_ALLOWED"})
    void failsWaitingRequestWithoutCallingFastApi(RepresentativeState state, String errorCode) {
        var waiter = new ClaimedTask("analysis", "token", 92L, 1, EXPIRES_AT, "{}", null, false,
                null, false, 91L, 1, null);
        when(store.claimNext(any(), eq(Duration.ofSeconds(38)))).thenReturn(Optional.of(waiter));
        when(store.findRepresentative(eq(waiter), any())).thenReturn(new RepresentativeOutcome(state, null));

        worker().runNext();

        verify(store).fail(waiter, errorCode, true);
        verifyNoInteractions(client, validator, challengeService);
    }

    private ClaimedTask claim(String body, boolean submitAgain) {
        var task = new ClaimedTask("analysis", "token", 91L, 1, EXPIRES_AT, "{}", body, submitAgain, null, false, 91L, 1, null);
        when(store.claimNext(any(), eq(Duration.ofSeconds(38)))).thenReturn(Optional.of(task));
        return task;
    }

    private ArtworkGenerationWorker worker() {
        return new ArtworkGenerationWorker(store, client, validator, challengeService,
                Duration.ofSeconds(3), Duration.ofSeconds(30));
    }
}
