package com.moamu.domain.artwork.client;

import java.net.URI;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.moamu.domain.artwork.client.ArtworkGenerationClientException.Reason;
import com.moamu.domain.artwork.dto.ArtworkGenerationRequest;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult;
import com.moamu.global.client.AiClientSupport;

@Component
public class ArtworkGenerationClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String serverUrl;

    public ArtworkGenerationClient(@Qualifier("photoVerificationRestClient") RestClient restClient,
            ObjectMapper objectMapper, @Value("${app.ai.server-url}") String serverUrl) {
        this.restClient = restClient;
        this.serverUrl = serverUrl;
        this.objectMapper = AiClientSupport.responseMapper(objectMapper);
        this.objectMapper.coercionConfigFor(LogicalType.Integer)
                .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
        this.objectMapper.coercionConfigFor(LogicalType.Boolean)
                .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
    }

    // 응답을 잃어도 같은 본문으로 다시 보낼 수 있도록 JSON을 먼저 만든다.
    public String serializeRequest(ArtworkGenerationRequest request) {
        try {
            return objectMapper.writer().without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("명화 생성 요청을 JSON으로 만들 수 없습니다.", exception);
        }
    }

    public void submit(String requestBody) {
        try {
            var response = restClient.post().uri(endpoint(""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve().toBodilessEntity();
            if (response.getStatusCode() != HttpStatus.ACCEPTED) {
                throw invalidResponse(response.getStatusCode().value(), null);
            }
        } catch (RestClientResponseException exception) {
            throw httpError(exception, HttpStatus.CONFLICT, Reason.PAYLOAD_CONFLICT);
        } catch (RestClientException exception) {
            throw new ArtworkGenerationClientException(Reason.CONNECTION_ERROR, null, exception);
        }
    }

    public ArtworkGenerationResult getStatus(long taskId, int taskVersion) {
        try {
            var response = restClient.get().uri(endpoint("/" + taskId + "/versions/" + taskVersion))
                    .accept(MediaType.APPLICATION_JSON).retrieve().toEntity(String.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw invalidResponse(response.getStatusCode().value(), null);
            }
            StatusResponse envelope = objectMapper.readValue(response.getBody(), StatusResponse.class);
            ArtworkGenerationResult result = envelope == null ? null : envelope.data();
            // 다른 작업이나 이전 버전의 결과가 섞이지 않도록 확인한다.
            if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || result == null
                    || !Objects.equals(result.taskId(), taskId) || !Objects.equals(result.taskVersion(), taskVersion)
                    || !hasRequiredFields(result)) {
                throw invalidResponse(HttpStatus.OK.value(), null);
            }
            return result;
        } catch (RestClientResponseException exception) {
            throw httpError(exception, HttpStatus.NOT_FOUND, Reason.TASK_NOT_FOUND);
        } catch (RestClientException exception) {
            throw new ArtworkGenerationClientException(Reason.CONNECTION_ERROR, null, exception);
        } catch (JsonProcessingException exception) {
            throw invalidResponse(HttpStatus.OK.value(), exception);
        }
    }

    private boolean hasRequiredFields(ArtworkGenerationResult result) {
        if (result.status() == null) {
            return false;
        }
        return switch (result.status()) {
            case PENDING, RUNNING -> true;
            case FAILED -> result.errorCode() != null && !result.errorCode().isBlank() && result.retryable() != null;
            case SUCCEEDED -> result.image() != null && result.source() != null && result.reuseAllowed() != null;
        };
    }

    private URI endpoint(String path) {
        return AiClientSupport.endpoint(serverUrl, "/internal/artwork-generations" + path);
    }

    private ArtworkGenerationClientException httpError(RestClientResponseException exception,
            HttpStatus knownStatus, Reason knownReason) {
        int status = exception.getStatusCode().value();
        Reason reason = status == knownStatus.value() ? knownReason : Reason.HTTP_ERROR;
        return new ArtworkGenerationClientException(reason, status, exception);
    }

    private ArtworkGenerationClientException invalidResponse(int status, Throwable cause) {
        return new ArtworkGenerationClientException(Reason.INVALID_RESPONSE, status, cause);
    }

    private record StatusResponse(Boolean success, ArtworkGenerationResult data) {}
}
