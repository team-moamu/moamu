package com.moamu.domain.verification.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.moamu.domain.verification.dto.PhotoVerificationErrorResponse;
import com.moamu.domain.verification.dto.PhotoVerificationRequest;
import com.moamu.domain.verification.dto.PhotoVerificationResponse;
import com.moamu.domain.verification.exception.PhotoVerificationTechnicalException;
import jakarta.validation.Validator;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// AI 서버에 사진 판정 요청
@Component
public class PhotoVerificationClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final String serverUrl;

    // 생성자에서 필요한 객체 받기
    public PhotoVerificationClient(
            @Qualifier("photoVerificationRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            Validator validator,
            @Value("${app.ai.server-url}") String serverUrl) {
        this.restClient = restClient;
        this.validator = validator;
        this.serverUrl = serverUrl;

        // 외부 응답 검증 설정이 프로젝트의 다른 JSON 처리에 영향을 주지 않도록 복사
        this.objectMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        this.objectMapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
    }

    // 요청 검증 후 AI 서버에 사진 판정을 요청하고 검증된 응답 반환
    public PhotoVerificationResponse verify(PhotoVerificationRequest request) {
        validateFields(request, "AI 판정 요청의 필수 값이 올바르지 않습니다.");
        Set<String> expectedCodes = new HashSet<>();
        for (var condition : request.conditions()) {
            if (!expectedCodes.add(condition.code())) {
                throw new PhotoVerificationTechnicalException("AI 판정 요청의 조건 코드가 중복되었습니다.");
            }
        }

        try {
            var entity = restClient.post()
                    .uri(endpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);
            if (entity.getStatusCode() != HttpStatus.OK) {
                throw new PhotoVerificationTechnicalException("AI 판정 성공 응답의 HTTP 상태가 올바르지 않습니다.");
            }

            PhotoVerificationResponse response = read(entity.getBody(), PhotoVerificationResponse.class);
            validateFields(response, "AI 판정 응답의 필수 값이 올바르지 않습니다.");
            validateIds(request, response.aiTaskId(), response.verificationId());
            if (!"SUCCEEDED".equals(response.processingStatus())) {
                throw new PhotoVerificationTechnicalException("AI 판정 응답의 처리 상태가 올바르지 않습니다.");
            }

            Set<String> actualCodes = new HashSet<>();
            for (var check : response.checks()) {
                if (!actualCodes.add(check.code())) {
                    throw new PhotoVerificationTechnicalException("AI 판정 응답의 조건 코드가 중복되었습니다.");
                }
            }
            if (!expectedCodes.equals(actualCodes)) {
                throw new PhotoVerificationTechnicalException("AI 판정 응답에 조건 코드가 누락되거나 추가되었습니다.");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw remoteError(request, exception);
        } catch (RestClientException exception) {
            throw new PhotoVerificationTechnicalException("AI 판정 서버와 통신하지 못했습니다.", exception);
        }
    }

    // AI 오류 응답을 검증하고 기술 오류 예외 생성
    private PhotoVerificationTechnicalException remoteError(
            PhotoVerificationRequest request, RestClientResponseException exception) {
        PhotoVerificationErrorResponse response = read(
                exception.getResponseBodyAsString(), PhotoVerificationErrorResponse.class);
        validateFields(response, "AI 오류 응답의 필수 값이 올바르지 않습니다.");
        validateErrorIds(request, response, exception.getStatusCode().value());
        int expectedStatus = switch (response.error().code()) {
            case "INVALID_REQUEST" -> 400;
            case "UNSUPPORTED_IMAGE", "IMAGE_FETCH_FAILED" -> 422;
            case "INTERNAL_ERROR" -> 500;
            case "MODEL_UNAVAILABLE" -> 503;
            default -> -1;
        };
        if (!"ERROR".equals(response.processingStatus())
                || exception.getStatusCode().value() != expectedStatus) {
            throw new PhotoVerificationTechnicalException("AI 오류 응답의 처리 상태 또는 오류 코드가 올바르지 않습니다.");
        }
        return new PhotoVerificationTechnicalException(
                "AI 판정 기술 오류: " + response.error().code(), exception);
    }

    // AI 서버 기본 주소를 검증하고 사진 판정 API의 전체 주소 생성
    private URI endpoint() {
        try {
            URI base = URI.create(serverUrl == null ? "" : serverUrl.strip());
            if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                    || base.getHost() == null || base.getRawQuery() != null
                    || base.getRawFragment() != null || base.getRawUserInfo() != null) {
                throw new IllegalArgumentException("AI_SERVER_URL must be an HTTP(S) server URL");
            }
            return URI.create(base.toString().replaceAll("/+$", "") + "/internal/photo-verifications");
        } catch (IllegalArgumentException exception) {
            throw new PhotoVerificationTechnicalException("AI_SERVER_URL 설정이 필요합니다.", exception);
        }
    }

    // JSON 응답을 지정한 DTO로 변환하고 빈 응답/형식 오류 처리
    private <T> T read(String body, Class<T> type) {
        if (body == null || body.isBlank()) {
            throw new PhotoVerificationTechnicalException("AI 판정 응답이 비어 있습니다.");
        }
        try {
            return objectMapper.readValue(body, type);
        } catch (JsonProcessingException exception) {
            throw new PhotoVerificationTechnicalException("AI 판정 응답 형식이 올바르지 않습니다.", exception);
        }
    }

    // DTO의 필수 값과 길이 등 검증 조건 확인 ㅇ
    private void validateFields(Object value, String message) {
        if (value == null || !validator.validate(value).isEmpty()) {
            throw new PhotoVerificationTechnicalException(message);
        }
    }

    // 요청을 읽지 못한 400 오류에 한해서 식별자 null 허용.
    // 반환된 식별자가 있다면 반드시 원 요청과 일치해야 한다.
    private void validateErrorIds(PhotoVerificationRequest request,
            PhotoVerificationErrorResponse response, int httpStatus) {
        if (httpStatus == 400 && "INVALID_REQUEST".equals(response.error().code())) {
            if ((response.aiTaskId() != null && !request.aiTaskId().equals(response.aiTaskId()))
                    || (response.verificationId() != null
                    && !request.verificationId().equals(response.verificationId()))) {
                throw new PhotoVerificationTechnicalException("AI 판정 요청과 응답의 ID가 일치하지 않습니다.");
            }
            return;
        }
        validateIds(request, response.aiTaskId(), response.verificationId());
    }

    // 요청과 응답의 작업 ID,인증 ID 일치 여부 확인
    private void validateIds(PhotoVerificationRequest request, String aiTaskId, String verificationId) {
        if (!request.aiTaskId().equals(aiTaskId) || !request.verificationId().equals(verificationId)) {
            throw new PhotoVerificationTechnicalException("AI 판정 요청과 응답의 ID가 일치하지 않습니다.");
        }
    }
}
