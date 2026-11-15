package com.moamu.domain.goal;

import com.moamu.domain.goal.client.GoalEvaluationClient;
import com.moamu.domain.goal.dto.GoalAnalysisRequest;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalEvaluationRequest;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.dto.GoalEvaluationResponse;
import com.moamu.domain.goal.dto.GoalEvaluationResponse.Status;
import com.moamu.domain.goal.repository.GoalAnalysisStore;
import com.moamu.domain.goal.repository.GoalCategoryRepository;
import com.moamu.domain.goal.service.GoalAnalysisService;
import com.moamu.global.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalAnalysisServiceTests {

    @Mock private GoalEvaluationClient evaluationClient;
    @Mock private GoalCategoryRepository categoryRepository;
    @Mock private GoalAnalysisStore analysisStore;
    @InjectMocks private GoalAnalysisService service;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void storesOwnerOriginalGoalAndExistingCategoryOrNewPath(boolean existingCategory) {
        String goal = "  영어 단어 20개 외우기  ";
        var path = new CategoryPath("학습", "외국어", "어휘 학습");
        var evaluation = new GoalEvaluationResponse(Status.VERIFIABLE, "Daily vocabulary study", path,
                List.of("학습 흔적이 보여야 해요."), "단어장을 촬영해 주세요.", "어휘 학습 흔적을 확인합니다.", List.of());
        when(evaluationClient.evaluate(new GoalEvaluationRequest(goal, null))).thenReturn(evaluation);
        when(categoryRepository.findActiveMinorId(path.major(), path.middle(), path.minor()))
                .thenReturn(existingCategory ? Optional.of(3L) : Optional.empty());
        when(analysisStore.save(any())).thenReturn("analysis-id");

        var response = service.analyze(7L, new GoalAnalysisRequest(goal));

        var snapshot = ArgumentCaptor.forClass(GoalAnalysisSnapshot.class);
        verify(analysisStore).save(snapshot.capture());
        assertThat(snapshot.getValue().userId()).isEqualTo(7L);
        assertThat(snapshot.getValue().goal()).isEqualTo(goal);
        assertThat(snapshot.getValue().categoryId()).isEqualTo(existingCategory ? 3L : null);
        assertThat(snapshot.getValue().categoryPath()).isEqualTo(path);
        assertThat(snapshot.getValue().conditions()).isEqualTo(evaluation.conditions());
        assertThat(response.analysisId()).isEqualTo("analysis-id");
        assertThat(response.goal()).isEqualTo(goal);
        assertThat(response.normalizedGoal()).isEqualTo(evaluation.normalizedGoal());
        assertThat(snapshot.getValue().normalizedGoal()).isEqualTo(evaluation.normalizedGoal());
        assertThat(response.alternatives()).isNull();
    }

    @Test
    void notVerifiableDoesNotQueryCategoriesOrStoreAnalysis() {
        var alternatives = List.of("감사 일기 쓰기", "고마운 순간 기록하기", "감사 편지 쓰기");
        when(evaluationClient.evaluate(any())).thenReturn(new GoalEvaluationResponse(
                Status.NOT_VERIFIABLE, null, null, List.of(), null, null, alternatives));

        var result = service.analyze(7L, new GoalAnalysisRequest("긍정적으로 생각하기"));

        assertThat(result.status()).isEqualTo(Status.NOT_VERIFIABLE);
        assertThat(result.alternatives()).isEqualTo(alternatives);
        assertThat(result.analysisId()).isNull();
        verifyNoInteractions(categoryRepository, analysisStore);
    }

    @Test
    void rejectsMissingOwnerBeforeCallingAi() {
        assertThatThrownBy(() -> service.analyze(null, new GoalAnalysisRequest("산책하기")))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(evaluationClient, categoryRepository, analysisStore);
    }
}
