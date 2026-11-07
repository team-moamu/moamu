package com.moamu.domain.verification.controller;

import com.moamu.domain.verification.VerificationService;
import com.moamu.domain.verification.dto.ChallengeVerificationResponseDto;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import com.moamu.global.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VerificationControllerTests {

    private VerificationService verificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        verificationService = mock(VerificationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new VerificationController(verificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsCommonResponseWithAllFieldsAndConditionList() throws Exception {
        when(verificationService.getVerification(10L, 20L)).thenReturn(
                new ChallengeVerificationResponseDto(10L, "Reading", 20L, 3, 7, 2,
                        List.of("Show the book", "Show the date")));

        mockMvc.perform(get("/api/verification").param("challengeId", "10").param("scheduleId", "20"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"success":true,"data":{"challengeId":10,"challengeTitle":"Reading",
                        "scheduleId":20,"dayNo":3,"dayCount":7,"attemptCount":2,
                        "description":["Show the book","Show the date"]}}
                        """));
        verify(verificationService).getVerification(10L, 20L);
    }

    @ParameterizedTest
    @CsvSource({"10,20", "10,99"})
    void returnsCommonNotFoundResponse(Long challengeId, Long scheduleId) throws Exception {
        when(verificationService.getVerification(challengeId, scheduleId))
                .thenThrow(new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/verification")
                        .param("challengeId", challengeId.toString()).param("scheduleId", scheduleId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,20", "10,NULL", "invalid,20", "10,invalid"}, nullValues = "NULL")
    void missingOrNonNumericIdsReturnCommonBadRequest(String challengeId, String scheduleId) throws Exception {
        var request = get("/api/verification");
        if (challengeId != null) {
            request.param("challengeId", challengeId);
        }
        if (scheduleId != null) {
            request.param("scheduleId", scheduleId);
        }

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        verifyNoInteractions(verificationService);
    }
}
