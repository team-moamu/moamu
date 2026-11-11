package com.moamu.domain.challenge.service;

import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.challenge.enums.Status;
import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.schedule.entity.Schedule;
import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.domain.schedule.service.ScheduleService;
import com.moamu.domain.verification.entity.Verification;
import com.moamu.domain.verification.repository.VerificationRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeImmediateCompletionTests {
    @Mock ChallengeRepository challenges;
    @Mock ScheduleRepository schedules;
    @Mock VerificationRepository verifications;
    @Mock ScheduleService scheduleService;
    @InjectMocks ChallengeService service;
    Challenge challenge;
    List<Schedule> rounds;

    @BeforeEach
    void setUp() {
        LocalDate end = LocalDate.now(ZoneId.of("Asia/Seoul"));
        challenge = Challenge.create(1L, 10L, 20L, 30L, "Test", "Goal", null,
            "Guide", 7, end.minusDays(6), end, "Scope", 3, 3);
        ReflectionTestUtils.setField(challenge, "status", Status.IN_PROGRESS);
        rounds = IntStream.range(0, 7).mapToObj(index -> {
            LocalDate date = end.minusDays(6 - index);
            Schedule round = Schedule.create(40L, index + 1, date,
                date.atStartOfDay(), date.plusDays(1).atStartOfDay(), index);
            ReflectionTestUtils.setField(round, "status", com.moamu.domain.schedule.enums.Status.SUCCESS);
            return round;
        }).toList();
        Verification verification = mock(Verification.class);
        when(verification.getScheduleId()).thenReturn(50L);
        when(verifications.findById(70L)).thenReturn(Optional.of(verification));
        when(schedules.findById(50L)).thenReturn(Optional.of(rounds.getLast()));
        when(challenges.findOwnedForSettlement(1L, 40L)).thenReturn(Optional.of(challenge));
    }

    @ParameterizedTest
    @CsvSource({"SUCCESS,1,true", "SUCCESS,3,true", "FAILED,3,true", "FAILED,2,false",
        "OPEN,2,false", "IN_REVIEW,3,false", "PROGRESSING,3,false"})
    void completesOnlyFinalLastRound(String status, int attempts, boolean completed) {
        when(schedules.findSchedulesForSettlement(40L)).thenReturn(rounds);
        ReflectionTestUtils.setField(rounds.getLast(), "status", com.moamu.domain.schedule.enums.Status.valueOf(status));
        ReflectionTestUtils.setField(rounds.getLast(), "attemptCount", attempts);
        service.completeAfterVerification(1L, 70L);
        assertThat(challenge.getStatus()).isEqualTo(completed ? Status.COMPLETED : Status.IN_PROGRESS);
        if (completed) {
            service.completeAfterVerification(1L, 70L);
            assertThat(challenge.getStatus()).isEqualTo(Status.COMPLETED);
        }
    }

    @Test
    void earlierPendingRoundBlocksCompletion() {
        when(schedules.findSchedulesForSettlement(40L)).thenReturn(rounds);
        ReflectionTestUtils.setField(rounds.getFirst(), "status", com.moamu.domain.schedule.enums.Status.IN_REVIEW);
        service.completeAfterVerification(1L, 70L);
        assertThat(challenge.getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    void unresolvedLatestVerificationBlocksCompletionEvenWithFinalSchedules() {
        when(schedules.findSchedulesForSettlement(40L)).thenReturn(rounds);
        when(verifications.countPendingLatestVerifications(eq(40L), anyList())).thenReturn(1L);
        service.completeAfterVerification(1L, 70L);
        assertThat(challenge.getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    void futureEndDateDoesNotComplete() {
        ReflectionTestUtils.setField(challenge, "endDate", LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1));
        service.completeAfterVerification(1L, 70L);
        assertThat(challenge.getStatus()).isEqualTo(Status.IN_PROGRESS);
        verify(schedules, never()).findSchedulesForSettlement(anyLong());
    }

    @ParameterizedTest
    @CsvSource({"READY,SUCCESS,1", "IN_PROGRESS,SUCCESS,1", "READY,FAILED,3", "IN_PROGRESS,FAILED,3"})
    void sixFailuresAndFinalResultCompleteRegardlessOfMissedStartTransition(
            String challengeStatus, String lastStatus, int attempts) {
        ReflectionTestUtils.setField(challenge, "status", Status.valueOf(challengeStatus));
        rounds.subList(0, 6).forEach(round -> ReflectionTestUtils.setField(round, "status",
            com.moamu.domain.schedule.enums.Status.FAILED));
        ReflectionTestUtils.setField(rounds.getLast(), "status", com.moamu.domain.schedule.enums.Status.valueOf(lastStatus));
        ReflectionTestUtils.setField(rounds.getLast(), "attemptCount", attempts);
        when(schedules.findSchedulesForSettlement(40L)).thenReturn(rounds);
        service.completeAfterVerification(1L, 70L);
        assertThat(challenge.getStatus()).isEqualTo(Status.COMPLETED);
    }
}
