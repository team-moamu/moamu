package com.moamu.domain.schedule.controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.moamu.domain.schedule.dto.ScheduleDetailResponse;
import com.moamu.domain.schedule.service.ScheduleService;



@RestController 
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/detail")
    public List<ScheduleDetailResponse> getSchedule(@RequestParam("challengeId") Long challengeId) {

        // 임시. 회원 기능 구현 후 수정 필요
        Long userId = 1L; 
        return scheduleService.getScheduleDetail(userId, challengeId);
    }
    
}
