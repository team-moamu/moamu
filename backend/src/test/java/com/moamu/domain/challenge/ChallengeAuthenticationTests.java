package com.moamu.domain.challenge;

import com.moamu.domain.challenge.controller.ChallengeController;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.domain.challenge.dto.CustomChallengeCreateResponse;
import com.moamu.domain.challenge.exception.ChallengeErrorCode;
import com.moamu.global.exception.BusinessException;
import com.moamu.domain.schedule.controller.ScheduleController;
import com.moamu.domain.schedule.service.ScheduleService;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.global.config.SecurityConfig;
import com.moamu.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ChallengeController.class, ScheduleController.class})
@Import(SecurityConfig.class)
class ChallengeAuthenticationTests {

    private static final String CUSTOM_REQUEST = """
        {"analysisId":"cc2e57ae-0f1b-49cf-9b2f-4c22f45f992b","startDate":"2026-11-16","endDate":"2026-11-22"}
        """;
    @Autowired MockMvc mockMvc;
    @MockitoBean ChallengeService challengeService;
    @MockitoBean ScheduleService scheduleService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserRepository userRepository;

    private void authenticateSecondUser() {
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(jwtTokenProvider.getUserId("user-two-token")).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
    }

    @Test
    void dailyChallengesUseAuthenticatedUserAndSelectedDate() throws Exception {
        authenticateSecondUser();
        LocalDate day = LocalDate.of(2026, 11, 7);
        when(challengeService.getTodayChallenges(2L, day, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/challenges/today")
                .header("Authorization", "Bearer user-two-token")
                .param("day", "2026-11-07").param("userId", "99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isEmpty());

        verify(challengeService).getTodayChallenges(2L, day, null);
        verifyNoMoreInteractions(challengeService);
    }

    @Test
    void scheduleHistoryUsesAuthenticatedUser() throws Exception {
        authenticateSecondUser();
        when(scheduleService.getScheduleDetail(2L, 10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/schedule/detail")
                .header("Authorization", "Bearer user-two-token")
                .param("challengeId", "10").param("userId", "99"))
            .andExpect(status().isOk());

        verify(scheduleService).getScheduleDetail(2L, 10L);
        verifyNoMoreInteractions(scheduleService);
    }

    @Test
    void challengeIsCreatedForAuthenticatedUser() throws Exception {
        authenticateSecondUser();

        mockMvc.perform(post("/api/challenges")
                .header("Authorization", "Bearer user-two-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"goalTemplateId": 20, "startDate": "2026-11-08", "endDate": "2026-11-14"}
                    """))
            .andExpect(status().isOk());

        verify(challengeService).createChallenge(eq(2L), any());
    }

    @Test
    void customChallengeUsesAuthenticatedOwnerAndReturnsChallengeId() throws Exception {
        authenticateSecondUser();
        when(challengeService.createCustomChallenge(eq(2L), any())).thenReturn(new CustomChallengeCreateResponse(100L));

        mockMvc.perform(post("/api/challenges/custom")
                .header("Authorization", "Bearer user-two-token")
                .contentType(MediaType.APPLICATION_JSON).content(CUSTOM_REQUEST))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.challengeId").value(100));

        verify(challengeService).createCustomChallenge(eq(2L), any());
    }

    @Test
    void generatedTaskLookupUsesTheAuthenticatedOwner() throws Exception {
        authenticateSecondUser();
        when(challengeService.getArtworkTask(2L, 91L)).thenReturn(new CustomChallengeCreateResponse(100L));

        mockMvc.perform(get("/api/challenges/custom/tasks/91").header("Authorization", "Bearer user-two-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.challengeId").value(100))
                .andExpect(jsonPath("$.data.artworkId").doesNotExist())
                .andExpect(jsonPath("$.data.imageKey").doesNotExist());
    }

    @Test
    void retryReturnsAcceptedAndUsesTheRequestedVersion() throws Exception {
        authenticateSecondUser();
        var accepted = java.time.Instant.parse("2026-11-24T04:00:00Z");
        when(challengeService.retryArtworkTask(2L, 91L, 1))
                .thenReturn(CustomChallengeCreateResponse.pending(91L, 2, accepted, accepted.plusSeconds(120)));

        mockMvc.perform(post("/api/challenges/custom/tasks/91/retry")
                .header("Authorization", "Bearer user-two-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedTaskVersion\":1}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.taskVersion").value(2))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"analysisId\":\"bad-id\",\"startDate\":\"2026-11-16\",\"endDate\":\"2026-11-22\"}",
        "{\"analysisId\":\"cc2e57ae-0f1b-49cf-9b2f-4c22f45f992b\",\"startDate\":\"2026-11-16\"}",
        "{\"analysisId\":\"cc2e57ae-0f1b-49cf-9b2f-4c22f45f992b\",\"startDate\":\"not-a-date\",\"endDate\":\"2026-11-22\"}"
    })
    void invalidCustomRequestReturns400(String body) throws Exception {
        authenticateSecondUser();
        mockMvc.perform(post("/api/challenges/custom")
                .header("Authorization", "Bearer user-two-token")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        verifyNoInteractions(challengeService);
    }

    @Test
    void expiredCustomAnalysisReturnsSpecified404() throws Exception {
        authenticateSecondUser();
        when(challengeService.createCustomChallenge(eq(2L), any()))
            .thenThrow(new BusinessException(ChallengeErrorCode.ANALYSIS_NOT_FOUND));

        mockMvc.perform(post("/api/challenges/custom")
                .header("Authorization", "Bearer user-two-token")
                .contentType(MediaType.APPLICATION_JSON).content(CUSTOM_REQUEST))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("ANALYSIS_NOT_FOUND"));
    }

    @Test
    void missingTokenRejectsRequestsBeforeCallingServices() throws Exception {
        mockMvc.perform(post("/api/challenges/custom").contentType(MediaType.APPLICATION_JSON).content(CUSTOM_REQUEST))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/challenges/today"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/schedule/detail").param("challengeId", "10"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(challengeService, scheduleService);
    }
}
