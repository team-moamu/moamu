package com.moamu.domain.goal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.goal.client.GoalEvaluationClient;
import com.moamu.domain.goal.dto.GoalEvaluationRequest;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.dto.GoalEvaluationResponse.Status;
import com.moamu.global.exception.BusinessException;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GoalEvaluationClientTests {

    private static final String SERVER_URL = "http://ai-server.test";
    private static final String ENDPOINT = SERVER_URL + "/internal/goals/evaluate-verifiability";
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private static final GoalEvaluationRequest REQUEST = new GoalEvaluationRequest("감사 일기 쓰기", null);
    private static final String SUCCESS_JSON = """
            {
              "status": "VERIFIABLE",
              "proposedCategoryPath": {"major": "생활", "middle": "기록", "minor": "일기 쓰기"},
              "conditions": ["일기를 쓴 흔적이 보여야 해요."],
              "photoGuide": "작성한 일기를 촬영해 주세요.",
              "verificationScope": "사진에 보이는 기록을 확인합니다.",
              "alternatives": []
            }
            """;

    private MockRestServiceServer server;
    private GoalEvaluationClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GoalEvaluationClient(builder.build(), new ObjectMapper(),
                VALIDATORS.getValidator(), SERVER_URL);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATORS.close();
    }

    @Test
    void sendsGoalWithExplicitNullCategoryAndReturnsNewPath() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"goal": "감사 일기 쓰기", "categoryPath": null}
                        """, true))
                .andRespond(withSuccess(SUCCESS_JSON, MediaType.APPLICATION_JSON));

        var response = client.evaluate(REQUEST);

        assertThat(response.status()).isEqualTo(Status.VERIFIABLE);
        assertThat(response.proposedCategoryPath().minor()).isEqualTo("일기 쓰기");
    }

    @Test
    void reusesExistingPathWithoutAcceptingAnotherProposedPath() {
        String response = SUCCESS_JSON.replace(
                "{\"major\": \"생활\", \"middle\": \"기록\", \"minor\": \"일기 쓰기\"}", "null");
        server.expect(requestTo(ENDPOINT))
                .andExpect(content().json("""
                        {"goal": "감사 일기 쓰기", "categoryPath": {
                          "major": "생활", "middle": "기록", "minor": "일기 쓰기"
                        }}
                        """, true))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        var result = client.evaluate(new GoalEvaluationRequest(REQUEST.goal(),
                new CategoryPath("생활", "기록", "일기 쓰기")));

        assertThat(result.proposedCategoryPath()).isNull();
    }

    @Test
    void returnsNotVerifiableAsNormalResult() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess("""
                {
                  "status": "NOT_VERIFIABLE", "proposedCategoryPath": null,
                  "conditions": [], "photoGuide": null, "verificationScope": null,
                  "alternatives": ["감사 일기 쓰기", "고마운 순간 기록하기", "감사 편지 쓰기"]
                }
                """, MediaType.APPLICATION_JSON));

        var result = client.evaluate(REQUEST);

        assertThat(result.status()).isEqualTo(Status.NOT_VERIFIABLE);
        assertThat(result.alternatives()).hasSize(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null", "", "{}",
            "{\"status\":0}",
            "{\"status\":\"NOT_VERIFIABLE\",\"conditions\":[],\"alternatives\":[\"하나\",\"둘\"]}",
            "{\"status\":\"NOT_VERIFIABLE\",\"conditions\":[],\"alternatives\":[1,2,3]}"
    })
    void rejectsMalformedResponses(String response) {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.evaluate(REQUEST))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsVerifiableResponseWithoutRequiredGuide() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(
                SUCCESS_JSON.replace("작성한 일기를 촬영해 주세요.", " "), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.evaluate(REQUEST))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void wrapsRemoteFailureAndTimeoutAsTechnicalError() {
        server.expect(requestTo(ENDPOINT)).andRespond(withServerError());
        server.expect(requestTo(ENDPOINT)).andRespond(withException(new SocketTimeoutException()));

        assertThatThrownBy(() -> client.evaluate(REQUEST))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .getErrorCode().getCode()).isEqualTo("AI_TECHNICAL_ERROR"));
        assertThatThrownBy(() -> client.evaluate(REQUEST))
                .isInstanceOf(BusinessException.class);
    }
}
