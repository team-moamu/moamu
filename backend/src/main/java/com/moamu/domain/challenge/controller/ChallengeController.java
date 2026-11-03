package com.moamu.domain.challenge.controller;

import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
import com.moamu.domain.challenge.dto.TodayChallengeResponse;
import com.moamu.domain.challenge.service.ChallengeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @PostMapping
    public void createChallenge( @Valid @RequestBody ChallengeCreateRequest request) {
        // 임시. 회원 기능 구현 후 수정 필요
        Long userId = 1L; 

        challengeService.createChallenge(userId, request);
    }

    @GetMapping("/today")
    public List<TodayChallengeResponse> getTodayChallenges(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day, 
        @RequestParam(required = false) String sortType
    ) {
        // 임시. 회원 기능 구현 후 수정 필요
        Long userId = 1L; 

        return challengeService.getTodayChallenges(userId, day, sortType);
    }
    
}