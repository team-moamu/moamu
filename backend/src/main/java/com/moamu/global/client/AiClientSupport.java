package com.moamu.global.client;

import java.net.URI;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

public final class AiClientSupport {

    private AiClientSupport() {
    }

    // AI 응답에만 이 검증 규칙을 적용하도록 기존 설정을 복사.
    public static ObjectMapper responseMapper(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
        return mapper;
    }

    public static URI endpoint(String serverUrl, String path) {
        URI base = URI.create(serverUrl == null ? "" : serverUrl.strip());
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getHost() == null || base.getRawQuery() != null
                || base.getRawFragment() != null || base.getRawUserInfo() != null) {
            throw new IllegalArgumentException("AI_SERVER_URL must be an HTTP(S) server URL");
        }
        return URI.create(base.toString().replaceAll("/+$", "") + path);
    }
}
