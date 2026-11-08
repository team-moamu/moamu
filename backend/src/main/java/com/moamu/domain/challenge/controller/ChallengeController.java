package com.moamu.domain.challenge.controller;

import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
import com.moamu.domain.challenge.dto.ChallengeCompletionResponse;
import com.moamu.domain.challenge.dto.ChallengeSettlementResponse;
import com.moamu.domain.challenge.dto.TodayChallengeResponse;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    // 완료된 도전의 작품 및 최종 퍼즐 집계 조회
    @GetMapping("/{challengeId}/completion")
    public ApiResponse<ChallengeCompletionResponse> getCompletion(
        @AuthenticationPrincipal Long userId,
        @PathVariable("challengeId") Long challengeId
    ) {
        return ApiResponse.success(challengeService.getCompletion(userId, challengeId));
    }

    // 로그인 사용자가 소유한 도전의 기간 종료 처리. 반복 호출해도 중복 반영하지 않음
    @PostMapping("/{challengeId}/settlement")
    public ApiResponse<ChallengeSettlementResponse> settle(
        @AuthenticationPrincipal Long userId,
        @PathVariable("challengeId") Long challengeId
    ) {
        return ApiResponse.success(challengeService.settle(userId, challengeId));
    }

    @PostMapping
    public void createChallenge( @Valid @RequestBody ChallengeCreateRequest request) {
        // 임시. 회원 기능 구현 후 수정 필요
        Long userId = 1L; 

        challengeService.createChallenge(userId, request);
    }

    @GetMapping("/today")
    public ApiResponse<List<TodayChallengeResponse>> getTodayChallenges(
        @RequestParam(name = "day", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day, 
        @RequestParam(name = "sortType", required = false) String sortType
    ) {
        // 임시. 회원 기능 구현 후 수정 필요
        Long userId = 1L; 
        List<TodayChallengeResponse> challenges = challengeService.getTodayChallenges(userId, day, sortType);

        return ApiResponse.success(challenges);
    }
    
}
