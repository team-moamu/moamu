package com.moamu.domain.challenge.service;

import com.moamu.domain.challenge.dto.TodayChallengeResponse;
import com.moamu.domain.challenge.repository.ChallengeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodayChallengeServiceTests {

    private static final Long USER_ID = 42L;
    private static final LocalDate DAY = LocalDate.of(2026, 11, 3);

    @Mock private ChallengeRepository challengeRepository;
    @InjectMocks private ChallengeService challengeService;

    // 지정한 날짜와 정규화한 정렬 기준을 저장소에 전달한다.
    @ParameterizedTest
    @CsvSource(value = {
            "name, name",
            "latest, latest",
            "NULL, latest",
            "'', latest",
            "unknown, latest",
            "NAME, latest"
    }, nullValues = "NULL")
    void forwardsRequestedDateAndNormalizesSort(String sort, String expectedSort) {
        TodayChallengeResponse first = new TodayChallengeResponse(
                100L, "Reading", DAY, DAY.plusDays(6),1L,  1, null, 0, null, 1L, com.moamu.domain.challenge.enums.Status.IN_PROGRESS);
        TodayChallengeResponse second = new TodayChallengeResponse(
                99L, "Exercise", DAY, DAY.plusDays(13),1L,  1, null, 0, null, 2L, com.moamu.domain.challenge.enums.Status.COMPLETED);
        when(challengeRepository.findTodayChallenges(USER_ID, DAY, expectedSort))
                .thenReturn(List.of(first, second));

        List<TodayChallengeResponse> result = challengeService.getTodayChallenges(USER_ID, DAY, sort);

        assertThat(result).extracting(TodayChallengeResponse::challengeId).containsExactly(100L, 99L);
        assertThat(result).extracting(TodayChallengeResponse::goalTemplateId).containsExactly(1L, 2L);
        assertThat(result).extracting(TodayChallengeResponse::challengeStatus).containsExactly(
                com.moamu.domain.challenge.enums.Status.IN_PROGRESS,
                com.moamu.domain.challenge.enums.Status.COMPLETED);
        verify(challengeRepository).findTodayChallenges(USER_ID, DAY, expectedSort);
        verifyNoMoreInteractions(challengeRepository);
    }

    // UTC에서는 전날인 시각에도 서울 기준 오늘을 사용한다.
    @Test
    void usesTodayInSeoulWhenDateIsMissing() {
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-02T15:30:00Z"), seoul);
        when(challengeRepository.findTodayChallenges(USER_ID, DAY, "name"))
                .thenReturn(List.of());

        try (MockedStatic<Clock> clocks = mockStatic(Clock.class)) {
            clocks.when(() -> Clock.system(seoul)).thenReturn(fixedClock);

            List<TodayChallengeResponse> result = challengeService.getTodayChallenges(USER_ID, null, "name");

            assertThat(result).isEmpty();
            verify(challengeRepository).findTodayChallenges(USER_ID, DAY, "name");
            clocks.verify(() -> Clock.system(seoul));
        }
    }


    @Test
    void returnsEmptyListWhenNoChallengesExist() {
        when(challengeRepository.findTodayChallenges(USER_ID, DAY, "latest"))
                .thenReturn(List.of());

        List<TodayChallengeResponse> result = challengeService.getTodayChallenges(USER_ID, DAY, "latest");

        assertThat(result).isEmpty();
        verify(challengeRepository).findTodayChallenges(USER_ID, DAY, "latest");
    }
}
