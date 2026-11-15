package com.moamu.domain.goal.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.NotBlank;

public record GoalAnalysisRequest(
    @NotBlank @JsonDeserialize(using = GoalStringDeserializer.class) String goal
) {

    // 숫자나 boolean을 목표 문자열로 자동 변환하지 않는다.
    public static class GoalStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return context.reportInputMismatch(String.class, "goal은 문자열이어야 합니다.");
            }
            return parser.getText();
        }
    }
}
