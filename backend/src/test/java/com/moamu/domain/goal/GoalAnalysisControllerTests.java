package com.moamu.domain.goal;

import com.moamu.domain.goal.controller.GoalAnalysisController;
import com.moamu.domain.goal.dto.GoalAnalysisRequest;
import com.moamu.domain.goal.dto.GoalAnalysisResponse;
import com.moamu.domain.goal.dto.GoalEvaluationResponse;
import com.moamu.domain.goal.dto.GoalEvaluationResponse.Status;
import com.moamu.global.exception.BusinessException;

import com.moamu.domain.goal.service.GoalAnalysisService;
import com.moamu.global.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.moamu.domain.goal.exception.GoalAnalysisErrorCode.AI_TECHNICAL_ERROR;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GoalAnalysisControllerTests {

    @Mock private GoalAnalysisService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new GoalAnalysisController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(7L, null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesAuthenticatedOwnerAndReturnsNormalNotVerifiableResponse() throws Exception {
        when(service.analyze(7L, new GoalAnalysisRequest("긍정적으로 생각하기")))
                .thenReturn(GoalAnalysisResponse.notVerifiable(List.of("일기 쓰기", "편지 쓰기", "기록하기")));

        mvc.perform(post("/api/goals/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"긍정적으로 생각하기\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("NOT_VERIFIABLE"))
                .andExpect(jsonPath("$.data.alternatives.length()").value(3))
                .andExpect(jsonPath("$.data.analysisId").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"goal\":null}", "{\"goal\":\" \"}", "{\"goal\":123}",
            "{\"goal\":true}", "{\"goal\":[]}", "{\"goal\":{}}", "null", "{"})
    void invalidGoalReturns400WithoutAiCall(String body) throws Exception {
        mvc.perform(post("/api/goals/analyze").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void returnsOriginalAndNormalizedGoalSeparately() throws Exception {
        var evaluation = new GoalEvaluationResponse(
                Status.VERIFIABLE,
                "Daily vocabulary study", null, List.of("Study notes must be visible"),
                "Photograph your notes", "Check visible study notes", List.of());
        when(service.analyze(7L, new GoalAnalysisRequest("Study 20 words after work")))
                .thenReturn(GoalAnalysisResponse.verifiable("analysis-id", "Study 20 words after work", evaluation));

        mvc.perform(post("/api/goals/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"Study 20 words after work\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goal").value("Study 20 words after work"))
                .andExpect(jsonPath("$.data.normalizedGoal").value("Daily vocabulary study"));
    }

    @Test
    void oversizedGoalReturns400BeforeAnalysis() throws Exception {
        mvc.perform(post("/api/goals/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void aiFailureReturnsSpecified502Error() throws Exception {
        when(service.analyze(eq(7L), any())).thenThrow(new BusinessException(AI_TECHNICAL_ERROR));

        mvc.perform(post("/api/goals/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"산책하기\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("AI_TECHNICAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("목표 분석 중 기술 오류가 발생했습니다."));
    }
}
