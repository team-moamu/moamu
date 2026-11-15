package com.moamu.domain.goal.service;

import org.springframework.stereotype.Service;

import com.moamu.domain.goal.client.GoalEvaluationClient;
import com.moamu.domain.goal.dto.GoalAnalysisRequest;
import com.moamu.domain.goal.dto.GoalAnalysisResponse;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalEvaluationRequest;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.dto.GoalEvaluationResponse;
import com.moamu.domain.goal.dto.GoalEvaluationResponse.Status;
import com.moamu.domain.goal.repository.GoalAnalysisStore;
import com.moamu.domain.goal.repository.GoalCategoryRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoalAnalysisService {

    private final GoalEvaluationClient goalEvaluationClient;
    private final GoalCategoryRepository goalCategoryRepository;
    private final GoalAnalysisStore goalAnalysisStore;

    // 자유목표 분석
    public GoalAnalysisResponse analyze(Long userId, GoalAnalysisRequest request) {
        // 로그인 사용자 및 입력 목표 검증
        if (userId == null || userId <= 0) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        if (request == null || request.goal() == null || request.goal().isBlank() || request.goal().length() > 500) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        // 사용자 목표를 AI에 전달해 사진 인증 가능 여부와 분류 경로 분석
        GoalEvaluationRequest evaluationRequest = new GoalEvaluationRequest(request.goal(), null);
        GoalEvaluationResponse result = goalEvaluationClient.evaluate(evaluationRequest);

        // 사진 인증이 어려운 목표는 대안 목표 반환
        if (result.status() == Status.NOT_VERIFIABLE) {
            return GoalAnalysisResponse.notVerifiable(result.alternatives());
        }

        // AI가 반환한 대·중·소분류 경로에 해당하는 기존 소분류 조회
        CategoryPath categoryPath = result.proposedCategoryPath();
        Long categoryId = goalCategoryRepository
            .findActiveMinorId(
                categoryPath.major().strip(),
                categoryPath.middle().strip(),
                categoryPath.minor().strip()
            )
            .orElse(null);

        // 도전 확정에 사용할 사용자별 분석 결과 구성
        GoalAnalysisSnapshot snapshot = new GoalAnalysisSnapshot(
            userId,
            request.goal(),
            result.normalizedGoal(),
            categoryId,
            result.proposedCategoryPath(),
            result.conditions(),
            result.photoGuide(),
            result.verificationScope()
        );

        // 분석 결과를 Redis에 저장한 뒤 분석 ID와 인증 정보 반환
        String analysisId = goalAnalysisStore.save(snapshot);
        return GoalAnalysisResponse.verifiable(analysisId, request.goal(), result);
    }
}
