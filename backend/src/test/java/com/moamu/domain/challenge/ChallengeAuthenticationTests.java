package com.moamu.domain.challenge;

import com.moamu.domain.challenge.controller.ChallengeController;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.domain.schedule.controller.ScheduleController;
import com.moamu.domain.schedule.service.ScheduleService;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.global.config.SecurityConfig;
import com.moamu.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
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
                    {"goalTemplateId": 20, "durationDays": 7, "startDate": "2026-11-08"}
                    """))
            .andExpect(status().isOk());

        verify(challengeService).createChallenge(eq(2L), any());
    }

    @Test
    void missingTokenRejectsRequestsBeforeCallingServices() throws Exception {
        mockMvc.perform(get("/api/challenges/today"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/schedule/detail").param("challengeId", "10"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(challengeService, scheduleService);
    }
}
