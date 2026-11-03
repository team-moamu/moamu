package com.moamu.domain.challenge.controller;

import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.challenge.enums.Status;
import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.global.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChallengeStopTests {
    private ChallengeRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = mock(ChallengeRepository.class);
        ChallengeService service = new ChallengeService(null, repository, null, null, null);
        mvc = MockMvcBuilders.standaloneSetup(new ChallengeController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void missingChallengeReturns404() throws Exception {
        when(repository.findById(10L)).thenReturn(Optional.empty());
        mvc.perform(patch("/api/challenges/10/stop").param("userId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void anotherOwnersChallengeReturns403() throws Exception {
        Challenge challenge = challenge(Status.IN_PROGRESS);
        mvc.perform(patch("/api/challenges/10/stop").param("userId", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        assertThat(challenge.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(challenge.getStoppedAt()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"READY", "SETTLING", "COMPLETED"})
    void invalidStateReturns400(Status initialStatus) throws Exception {
        Challenge challenge = challenge(initialStatus);
        mvc.perform(patch("/api/challenges/10/stop").param("userId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        assertThat(challenge.getStatus()).isEqualTo(initialStatus);
        assertThat(challenge.getStoppedAt()).isNull();
    }

    @Test
    void stopAndRepeatReturn204WithoutOverwritingTimestamp() throws Exception {
        Challenge challenge = challenge(Status.IN_PROGRESS);
        mvc.perform(patch("/api/challenges/10/stop").param("userId", "1"))
                .andExpect(status().isNoContent());
        assertThat(challenge.getStatus()).isEqualTo(Status.STOPPED);
        LocalDateTime stoppedAt = challenge.getStoppedAt();
        assertThat(stoppedAt).isNotNull();
        mvc.perform(patch("/api/challenges/10/stop").param("userId", "1"))
                .andExpect(status().isNoContent());
        assertThat(challenge.getStoppedAt()).isEqualTo(stoppedAt);
    }

    private Challenge challenge(Status status) {
        Challenge challenge = Challenge.create(1L, 1L, 1L, 1L, "title", "goal",
                null, null, 7, null, null, "scope", 3, 3);
        ReflectionTestUtils.setField(challenge, "status", status);
        when(repository.findById(10L)).thenReturn(Optional.of(challenge));
        return challenge;
    }
}
