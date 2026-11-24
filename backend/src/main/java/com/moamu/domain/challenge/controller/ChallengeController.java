package com.moamu.domain.challenge.controller;

import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
import com.moamu.domain.challenge.dto.ChallengeCompletionResponse;
import com.moamu.domain.challenge.dto.ChallengeSettlementResponse;
import com.moamu.domain.challenge.dto.TodayChallengeResponse;
import com.moamu.domain.challenge.dto.CustomChallengeCreateRequest;
import com.moamu.domain.challenge.dto.CustomChallengeCreateResponse;
import com.moamu.domain.challenge.dto.ArtworkTaskRetryRequest;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public void createChallenge(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody ChallengeCreateRequest request
    ) {
        challengeService.createChallenge(userId, request);
    }

    @GetMapping("/today")
    public ApiResponse<List<TodayChallengeResponse>> getTodayChallenges(
        @AuthenticationPrincipal Long userId,
        @RequestParam(name = "day", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day, 
        @RequestParam(name = "sortType", required = false) String sortType
    ) {
        List<TodayChallengeResponse> challenges = challengeService.getTodayChallenges(userId, day, sortType);

        return ApiResponse.success(challenges);
    }
    
    // 재사용할 작품이 있으면 도전을 만들고, 없으면 생성 작업을 접수.
    @PostMapping("/custom")
    public ResponseEntity<ApiResponse<CustomChallengeCreateResponse>> createCustomChallenge(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody CustomChallengeCreateRequest request
    ) {
        var response = challengeService.createCustomChallenge(userId, request);
        return artworkSubmissionResponse(response);
    }

    // 성공이면 challengeId
    // 진행 중이면 작업 ID, 버전, 기한
    // 실패면 오류 코드와 재시도 가능 여부 반환
    @GetMapping("/custom/tasks/{taskId}")
    public ApiResponse<CustomChallengeCreateResponse> getArtworkTask(
            @AuthenticationPrincipal Long userId, @PathVariable("taskId") long taskId) {
        return ApiResponse.success(challengeService.getArtworkTask(userId, taskId));
    }

    // 재시도 가능한 실패 작업이고 expectedTaskVersion이 맞으면 같은 작업 ID에서 버전을 반환
    @PostMapping("/custom/tasks/{taskId}/retry")
    public ResponseEntity<ApiResponse<CustomChallengeCreateResponse>> retryArtworkTask(
            @AuthenticationPrincipal Long userId, @PathVariable("taskId") long taskId,
            @Valid @RequestBody ArtworkTaskRetryRequest request) {
        return artworkSubmissionResponse(
                challengeService.retryArtworkTask(userId, taskId, request.expectedTaskVersion()));
    }

    // 최초 접수와 재시도의 HTTP 응답을 만드는 공통 메서드
    private ResponseEntity<ApiResponse<CustomChallengeCreateResponse>> artworkSubmissionResponse(
            CustomChallengeCreateResponse response) {
        HttpStatus status = switch (response.status()) {
            case "PENDING", "RUNNING" -> HttpStatus.ACCEPTED;
            default -> HttpStatus.OK;
        };
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }
}
