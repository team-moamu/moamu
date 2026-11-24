package com.moamu.domain.artwork.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moamu.domain.artwork.client.ArtworkGenerationClientException.Reason;
import com.moamu.domain.artwork.dto.ArtworkGenerationRequest;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult.Status;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;

class ArtworkGenerationClientTests {

    private static final String ENDPOINT = "http://localhost:8000/internal/artwork-generations";
    private static final String STATUS_ENDPOINT = ENDPOINT + "/91/versions/1";
    private MockRestServiceServer server;
    private ArtworkGenerationClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ArtworkGenerationClient(builder.build(), new ObjectMapper().registerModule(new JavaTimeModule()),
                "http://localhost:8000/");
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void submitsTheSavedBodyWithUtcDeadlineAndOriginalConditionOrder() {
        var request = new ArtworkGenerationRequest(91L, 1, Instant.parse("2026-11-23T04:02:00Z"), "Walk daily",
                new CategoryPath("Health", "Exercise", "Walking"), List.of("Shoes", "Trail"), "Show the trail");
        server.expect(requestTo(ENDPOINT)).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"taskId":91,"taskVersion":1,"expiresAt":"2026-11-23T04:02:00Z",
                         "normalizedGoal":"Walk daily","categoryPath":{"major":"Health","middle":"Exercise","minor":"Walking"},
                         "conditions":["Shoes","Trail"],"photoGuide":"Show the trail"}
                        """, true))
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        client.submit(client.serializeRequest(request));
    }

    @Test
    void sendsTheSameBodyAgainAfterALostResponse() {
        String savedBody = "{\"taskId\":91,\"taskVersion\":1,\"normalizedGoal\":\"매일 산책\"}";
        server.expect(requestTo(ENDPOINT)).andExpect(content().string(savedBody))
                .andRespond(withException(new IOException("Response lost")));
        server.expect(requestTo(ENDPOINT)).andExpect(content().string(savedBody))
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        assertReason(() -> client.submit(savedBody), Reason.CONNECTION_ERROR);
        client.submit(savedBody);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "RUNNING"})
    void readsProgressStatus(String status) {
        respondWithData("""
                {"taskId":91,"taskVersion":1,"status":"%s"}
                """.formatted(status));

        assertThat(client.getStatus(91L, 1).status()).isEqualTo(Status.valueOf(status));
    }

    @Test
    void readsFailureInformationFromSuccessfulStatusLookup() {
        respondWithData("""
                {"taskId":91,"taskVersion":1,"status":"FAILED","errorCode":"ARTWORK_GENERATION_FAILED","retryable":false}
                """);

        var result = client.getStatus(91L, 1);
        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.errorCode()).isEqualTo("ARTWORK_GENERATION_FAILED");
        assertThat(result.retryable()).isFalse();
    }

    @Test
    void readsImageAndSourceFromSuccessfulGeneration() {
        respondWithData("""
                {"taskId":91,"taskVersion":1,"status":"SUCCEEDED","reuseAllowed":true,
                 "image":{"key":"generated-artworks/91/1/result.webp","contentType":"image/webp",
                          "sizeBytes":123,"sha256":"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"},
                 "source":{"workId":"catalog-123","title":"Painting","sourceUrl":"https://example.org/work",
                           "rightsCode":"PUBLIC_DOMAIN","rightsUrl":"https://example.org/rights",
                           "checkedAt":"2026-11-23T04:00:10Z"}}
                """);

        var result = client.getStatus(91L, 1);
        assertThat(result.status()).isEqualTo(Status.SUCCEEDED);
        assertThat(result.image().key()).isEqualTo("generated-artworks/91/1/result.webp");
        assertThat(result.image().sizeBytes()).isEqualTo(123L);
        assertThat(result.source().checkedAt()).isEqualTo(Instant.parse("2026-11-23T04:00:10Z"));
        assertThat(result.reuseAllowed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"taskId\":92,\"taskVersion\":1,\"status\":\"RUNNING\"}",
        "{\"taskId\":91,\"taskVersion\":2,\"status\":\"RUNNING\"}",
        "{\"taskId\":\"91\",\"taskVersion\":1,\"status\":\"RUNNING\"}",
        "{\"taskId\":91,\"taskVersion\":1.0,\"status\":\"RUNNING\"}",
        "{\"taskId\":91,\"taskVersion\":1,\"status\":\"UNKNOWN\"}",
        "{\"taskId\":91,\"taskVersion\":1,\"status\":\"FAILED\",\"errorCode\":\"FAILED\"}",
        "{\"taskId\":91,\"taskVersion\":1,\"status\":\"FAILED\",\"errorCode\":\"FAILED\",\"retryable\":\"true\"}",
        "{\"taskId\":91,\"taskVersion\":1,\"status\":\"SUCCEEDED\"}"
    })
    void rejectsInvalidStatusData(String data) {
        respondWithData(data);

        assertReason(() -> client.getStatus(91L, 1), Reason.INVALID_RESPONSE);
    }

    @Test
    void distinguishesMissingTaskVersionFromOtherHttpErrors() {
        server.expect(requestTo(STATUS_ENDPOINT)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(STATUS_ENDPOINT)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertReason(() -> client.getStatus(91L, 1), Reason.TASK_NOT_FOUND);
        assertReason(() -> client.getStatus(91L, 1), Reason.HTTP_ERROR);
    }

    @Test
    void distinguishesPayloadConflictAndRequiresAcceptedStatus() {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.CONFLICT));
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.OK));

        assertReason(() -> client.submit("{}"), Reason.PAYLOAD_CONFLICT);
        assertReason(() -> client.submit("{}"), Reason.INVALID_RESPONSE);
    }

    private void respondWithData(String data) {
        server.expect(requestTo(STATUS_ENDPOINT)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"success\":true,\"data\":" + data + "}", MediaType.APPLICATION_JSON));
    }

    private void assertReason(Runnable request, Reason reason) {
        assertThatThrownBy(request::run).isInstanceOfSatisfying(ArtworkGenerationClientException.class,
                exception -> assertThat(exception.getReason()).isEqualTo(reason));
    }
}
