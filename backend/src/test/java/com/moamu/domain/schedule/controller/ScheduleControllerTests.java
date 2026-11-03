package com.moamu.domain.schedule.controller;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.moamu.domain.schedule.dto.ScheduleDetailResponse;
import com.moamu.domain.schedule.enums.Status;
import com.moamu.domain.schedule.service.ScheduleService;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import com.moamu.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** DB와 Security 필터를 제외한 요청 바인딩, 응답 직렬화, 예외 처리를 검증한다. */
class ScheduleControllerTests {

    private ScheduleService scheduleService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        scheduleService = mock(ScheduleService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ScheduleController(scheduleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                // standaloneSetup에도 Spring Boot 기본 날짜 형식(ISO 문자열)을 적용한다.
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
    }

    @Test
    void getScheduleReturnsScheduleList() throws Exception {
        when(scheduleService.getScheduleDetail(1L, 10L)).thenReturn(List.of(
                new ScheduleDetailResponse(10L, "Daily walk", 102L, 2,
                        LocalDate.of(2026, 11, 4), Status.UPCOMING),
                new ScheduleDetailResponse(10L, "Daily walk", 101L, 1,
                        LocalDate.of(2026, 11, 3), Status.OPEN)));

        mockMvc.perform(get("/api/schedule/detail").param("challengeId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].challengeId").value(10))
                .andExpect(jsonPath("$[0].challengeTitle").value("Daily walk"))
                .andExpect(jsonPath("$[0].scheduleId").value(102))
                .andExpect(jsonPath("$[0].dayNo").value(2))
                .andExpect(jsonPath("$[0].scheduleDate").value("2026-11-04"))
                .andExpect(jsonPath("$[0].scheduleStatus").value("UPCOMING"))
                .andExpect(jsonPath("$[1].scheduleId").value(101))
                .andExpect(jsonPath("$[1].scheduleStatus").value("OPEN"));

        verify(scheduleService).getScheduleDetail(1L, 10L);
    }

    @Test
    void getScheduleUsesTemporaryUserIdEvenWhenClientSuppliesUserId() throws Exception {
        when(scheduleService.getScheduleDetail(1L, 10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/schedule/detail")
                        .param("challengeId", "10").param("userId", "99"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // 회원 기능 연동 전까지 사용하는 임시 사용자 ID를 검증한다.
        verify(scheduleService).getScheduleDetail(1L, 10L);
        verifyNoMoreInteractions(scheduleService);
    }

    @Test
    void getScheduleReturnsEmptyArrayWhenNoSchedulesExist() throws Exception {
        when(scheduleService.getScheduleDetail(1L, 10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/schedule/detail").param("challengeId", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(scheduleService).getScheduleDetail(1L, 10L);
    }

    @Test
    void missingChallengeIdReturnsBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/schedule/detail"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void nonNumericChallengeIdReturnsBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/schedule/detail").param("challengeId", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void missingChallengeReturnsNotFound() throws Exception {
        when(scheduleService.getScheduleDetail(1L, 999L))
                .thenThrow(new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/schedule/detail").param("challengeId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        verify(scheduleService).getScheduleDetail(1L, 999L);
    }
}
