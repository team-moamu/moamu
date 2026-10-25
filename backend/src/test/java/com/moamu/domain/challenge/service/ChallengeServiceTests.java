package com.moamu.domain.challenge.service;

import com.moamu.domain.artwork.entity.Artwork;
import com.moamu.domain.artwork.repository.ArtworkRepository;
import com.moamu.domain.challenge.dto.ChallengeCreateRequest;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.goal.entity.GoalTemplate;
import com.moamu.domain.goal.entity.GoalTemplateArtwork;
import com.moamu.domain.goal.repository.GoalTemplateArtworkRepository;
import com.moamu.domain.goal.repository.GoalTemplateRepository;
import com.moamu.domain.schedule.entity.Schedule;
import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Spring 컨텍스트, DB, Security 없이 서비스의 생성 규칙만 검증합니다.
@ExtendWith(MockitoExtension.class)
class ChallengeServiceTests {

    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 25);

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private GoalTemplateRepository goalTemplateRepository;
    @Mock private ArtworkRepository artworkRepository;
    @Mock private GoalTemplateArtworkRepository goalTemplateArtworkRepository;
    @InjectMocks private ChallengeService challengeService;
    @Captor private ArgumentCaptor<Challenge> challengeCaptor;
    @Captor private ArgumentCaptor<List<Schedule>> schedulesCaptor;

    // 7·14·30일 각각에 대해 도전 정보, 종료일, 그리드 크기와 일별 회차 생성을 검증합니다.
    // CsvSource의 값은 순서대로 도전 기간, 기대 행 수, 기대 열 수입니다.
    @ParameterizedTest
    @CsvSource({"7, 3, 3", "14, 4, 4", "30, 5, 6"})
    void createsChallengeAndDailySchedules(int days, int rows, int cols) {
        stubTemplate();
        stubMapping();
        when(artworkRepository.findByArtworkIdAndDelYn(30L, "N")).thenReturn(Optional.of(mock(Artwork.class)));
        // JPA IDENTITY 저장 시 원본 엔티티에 ID가 할당되는 동작을 재현합니다.
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(invocation -> {
            Challenge challenge = invocation.getArgument(0);
            ReflectionTestUtils.setField(challenge, "challengeId", 100L);
            return challenge;
        });

        challengeService.createChallenge(1L, request(days));

        // 저장 대상으로 전달된 도전에 사용자·템플릿·작품 정보가 올바르게 반영되는지 확인합니다.
        verify(challengeRepository).save(challengeCaptor.capture());
        Challenge challenge = challengeCaptor.getValue();
        assertThat(challenge.getUserId()).isEqualTo(1L);
        assertThat(challenge.getCateId()).isEqualTo(20L);
        assertThat(challenge.getGoalTemplateId()).isEqualTo(10L);
        assertThat(challenge.getArtworkId()).isEqualTo(30L);
        assertThat(challenge.getChallengeTitle()).isEqualTo("Daily practice");
        assertThat(challenge.getGoalText()).isEqualTo("Practice for 10 minutes");
        assertThat(challenge.getPhotoGuide()).isEqualTo("Photograph the result");
        assertThat(challenge.getVerificationScope()).isEqualTo("Visible result only");
        // 시작일을 첫날로 포함한 종료일과 기간별 퍼즐 그리드 크기를 확인합니다.
        assertThat(challenge.getDurationDays()).isEqualTo(days);
        assertThat(challenge.getStartDate()).isEqualTo(START_DATE);
        assertThat(challenge.getEndDate()).isEqualTo(START_DATE.plusDays(days - 1));
        assertThat(challenge.getGridRows()).isEqualTo(rows);
        assertThat(challenge.getGridCols()).isEqualTo(cols);

        // 기간만큼 회차가 생성되고, 각 회차가 저장된 도전 ID를 참조하는지 확인합니다.
        verify(scheduleRepository).saveAll(schedulesCaptor.capture());
        List<Schedule> schedules = schedulesCaptor.getValue();
        assertThat(schedules).hasSize(days);
        for (int i = 0; i < days; i++) {
            Schedule schedule = schedules.get(i);
            LocalDate date = START_DATE.plusDays(i);
            assertThat(schedule.getChallengeId()).isEqualTo(100L);
            // 회차 번호는 1부터, 퍼즐 위치는 0부터 중복 없이 순서대로 부여되는지 확인합니다.
            assertThat(schedule.getDayNo()).isEqualTo(i + 1);
            assertThat(schedule.getPieceIndex()).isEqualTo(i);
            assertThat(schedule.getScheduleDate()).isEqualTo(date);
            // 인증 가능 시간은 해당 날짜의 자정부터 다음 날 자정까지로 설정되는지 확인합니다.
            assertThat(schedule.getStartAt()).isEqualTo(date.atStartOfDay());
            assertThat(schedule.getDeadlineAt()).isEqualTo(date.plusDays(1).atStartOfDay());
        }
    }

    // 7·14·30일 이외의 기간은 안내 메시지와 함께 거절하고, 작품 조회·저장을 진행하지 않는지 검증합니다.
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 6, 8, 13, 15, 29, 31})
    void rejectsUnsupportedDurationWithoutSaving(int days) {
        when(goalTemplateRepository.findByGoalTemplateIdAndDelYn(10L, "N")).thenReturn(Optional.of(mock(GoalTemplate.class)));

        assertThatThrownBy(() -> challengeService.createChallenge(1L, request(days)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST));

        verifyNoInteractions(challengeRepository, scheduleRepository,
                goalTemplateArtworkRepository, artworkRepository);
    }

    // 템플릿이 없으면 예외를 발생시키고, 작품 조회 및 도전·회차 저장을 하지 않는지 검증합니다.
    @Test
    void rejectsMissingTemplateWithoutSaving() {
        when(goalTemplateRepository.findByGoalTemplateIdAndDelYn(10L, "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> challengeService.createChallenge(1L, request(7)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));

        verifyNoInteractions(challengeRepository, scheduleRepository,
                goalTemplateArtworkRepository, artworkRepository);
    }

    // 삭제되지 않은 템플릿-작품 연결이 없으면 예외를 발생시키고, 작품 조회·저장을 하지 않는지 검증합니다.
    @Test
    void rejectsMissingMappingWithoutSaving() {
        when(goalTemplateRepository.findByGoalTemplateIdAndDelYn(10L, "N")).thenReturn(Optional.of(mock(GoalTemplate.class)));
        when(goalTemplateArtworkRepository.findByGoalTemplateIdAndDelYn(10L, "N"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> challengeService.createChallenge(1L, request(7)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));

        verifyNoInteractions(challengeRepository, scheduleRepository, artworkRepository);
    }

    // 연결 정보는 있지만 실제 작품이 없으면 예외를 발생시키고, 도전·회차를 저장하지 않는지 검증합니다.
    @Test
    void rejectsMissingArtworkWithoutSaving() {
        when(goalTemplateRepository.findByGoalTemplateIdAndDelYn(10L, "N")).thenReturn(Optional.of(mock(GoalTemplate.class)));
        stubMapping();
        when(artworkRepository.findByArtworkIdAndDelYn(30L, "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> challengeService.createChallenge(1L, request(7)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));

        verifyNoInteractions(challengeRepository, scheduleRepository);
    }

    // 기간만 바꿔 동일한 템플릿과 시작일로 테스트 요청을 만듭니다.
    private ChallengeCreateRequest request(int days) {
        return new ChallengeCreateRequest(10L, days, START_DATE);
    }

    // DB 조회 대신 반환할 템플릿 정보로, 생성된 도전에 복사되는 값을 검증할 때 사용합니다.
    private void stubTemplate() {
        GoalTemplate template = mock(GoalTemplate.class);
        when(template.getGoalTemplateId()).thenReturn(10L);
        when(template.getCateId()).thenReturn(20L);
        when(template.getGoalName()).thenReturn("Daily practice");
        when(template.getDescription()).thenReturn("Practice for 10 minutes");
        when(template.getPhotoGuide()).thenReturn("Photograph the result");
        when(template.getVerificationScope()).thenReturn("Visible result only");
        when(goalTemplateRepository.findByGoalTemplateIdAndDelYn(10L, "N")).thenReturn(Optional.of(template));
    }

    // 템플릿 10번에 삭제되지 않은 작품 30번 연결이 존재하는 상황을 재현합니다.
    private void stubMapping() {
        GoalTemplateArtwork mapping = mock(GoalTemplateArtwork.class);
        when(mapping.getArtworkId()).thenReturn(30L);
        when(goalTemplateArtworkRepository.findByGoalTemplateIdAndDelYn(10L, "N"))
                .thenReturn(Optional.of(mapping));
    }
}
