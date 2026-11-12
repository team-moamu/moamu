package com.moamu.domain.schedule.controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.moamu.domain.schedule.dto.ScheduleDetailResponse;
import com.moamu.domain.schedule.service.ScheduleService;
import com.moamu.global.response.ApiResponse;



@RestController 
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/detail")
    public ApiResponse<List<ScheduleDetailResponse>> getSchedule(
        @AuthenticationPrincipal Long userId,
        @RequestParam("challengeId") Long challengeId
    ) {
        List<ScheduleDetailResponse> schedules = scheduleService.getScheduleDetail(userId, challengeId);
        return ApiResponse.success(schedules);
    }
    
}
