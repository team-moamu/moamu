package com.moamu.domain.goal.client;

import java.net.URI;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.moamu.domain.goal.dto.GoalEvaluationRequest;
import com.moamu.domain.goal.dto.GoalEvaluationResponse;
import com.moamu.domain.goal.exception.GoalAnalysisErrorCode;
import com.moamu.global.exception.BusinessException;

import jakarta.validation.Validator;

@Component
public class GoalEvaluationClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final String serverUrl;

    public GoalEvaluationClient(
        @Qualifier("photoVerificationRestClient") RestClient restClient,
        ObjectMapper objectMapper,
        Validator validator,
        @Value("${app.ai.server-url}") String serverUrl
    ) {
        this.restClient = restClient;
        this.validator = validator;
        this.serverUrl = serverUrl;

        // 외부 응답 검증에 사용할 JSON 설정 구성
        this.objectMapper = objectMapper.copy()
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        this.objectMapper.coercionConfigFor(LogicalType.Textual)
            .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
            .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
            .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
    }

    // AI 서버에 목표 분석 요청 후 응답 검증
    public GoalEvaluationResponse evaluate(GoalEvaluationRequest request) {
        validateFields(request);

        try {
            // 목표 문장과 기존 분류 경로 전달
            ResponseEntity<String> entity = restClient.post()
                .uri(endpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

            if (entity.getStatusCode() != HttpStatus.OK
                    || entity.getBody() == null || entity.getBody().isBlank()) {
                throw new BusinessException(GoalAnalysisErrorCode.AI_TECHNICAL_ERROR);
            }

            // 응답 변환 및 인증 가능 여부에 따른 필수 정보 검증
            GoalEvaluationResponse response = objectMapper.readValue(
                entity.getBody(),
                GoalEvaluationResponse.class
            );
            validateFields(response);
            validateResult(request, response);

            return response;
        } catch (RestClientException | JsonProcessingException exception) {
            throw new BusinessException(GoalAnalysisErrorCode.AI_TECHNICAL_ERROR, exception);
        }
    }

    // 인증 가능 여부에 따른 인증 정보와 대안 목표 검증
    private void validateResult(GoalEvaluationRequest request, GoalEvaluationResponse response) {
        if (response.status() == GoalEvaluationResponse.Status.NOT_VERIFIABLE) {
            if (response.proposedCategoryPath() != null || !response.conditions().isEmpty()
                    || response.photoGuide() != null || response.verificationScope() != null
                    || response.alternatives().size() != 3) {
                throw new BusinessException(GoalAnalysisErrorCode.AI_TECHNICAL_ERROR);
            }
            return;
        }

        boolean needsCategoryPath = request.categoryPath() == null;
        if (needsCategoryPath != (response.proposedCategoryPath() != null)
                || response.conditions().isEmpty() || !response.alternatives().isEmpty()
                || response.photoGuide() == null || response.photoGuide().isBlank()
                || response.verificationScope() == null || response.verificationScope().isBlank()) {
            throw new BusinessException(GoalAnalysisErrorCode.AI_TECHNICAL_ERROR);
        }
    }

    // 요청 및 응답 DTO의 필수 값과 길이 검증
    private void validateFields(Object value) {
        if (value == null || !validator.validate(value).isEmpty()) {
            throw new BusinessException(GoalAnalysisErrorCode.AI_TECHNICAL_ERROR);
        }
    }

    // AI 서버 주소 검증 및 목표 분석 API 주소 생성
    private URI endpoint() {
        try {
            URI base = URI.create(serverUrl == null ? "" : serverUrl.strip());
            if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                    || base.getHost() == null || base.getRawQuery() != null
                    || base.getRawFragment() != null || base.getRawUserInfo() != null) {
                throw new IllegalArgumentException("AI_SERVER_URL must be an HTTP(S) server URL");
            }
            return URI.create(base.toString().replaceAll("/+$", "")
                + "/internal/goals/evaluate-verifiability");
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(GoalAnalysisErrorCode.AI_TECHNICAL_ERROR, exception);
        }
    }
}
