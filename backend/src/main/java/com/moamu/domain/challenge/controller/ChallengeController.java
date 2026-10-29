package com.moamu.domain.challenge.controller;

import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
import com.moamu.domain.challenge.service.ChallengeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
}