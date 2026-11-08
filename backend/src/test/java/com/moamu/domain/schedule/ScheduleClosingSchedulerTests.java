package com.moamu.domain.schedule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.challenge.repository.ChallengeRepository;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.domain.schedule.scheduler.ScheduleClosingScheduler;
import com.moamu.domain.schedule.service.ScheduleService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class ScheduleClosingSchedulerTests {

    private ApplicationContextRunner schedulerContext() {
        return new ApplicationContextRunner()
            .withUserConfiguration(ScheduleClosingScheduler.class)
            .withBean(ChallengeRepository.class, () -> mock(ChallengeRepository.class))
            .withBean(ScheduleService.class, () -> mock(ScheduleService.class))
            .withBean(ChallengeService.class, () -> mock(ChallengeService.class))
            .withPropertyValues("app.schedule-closing.delay-ms=3600000");
    }

    @Test
    void registersTimerInLocalProfileAndCanDisableIt() {
        schedulerContext().withPropertyValues("spring.profiles.active=local").run(context -> {
            assertThat(context).hasSingleBean(ScheduleClosingScheduler.class);
            assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).hasSize(1);
        });
        schedulerContext().withPropertyValues("spring.profiles.active=local", "app.schedule-closing.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(ScheduleClosingScheduler.class));
    }

    @Test
    void registersTimerInProductionButNotTestProfile() {
        schedulerContext().withPropertyValues("spring.profiles.active=prod")
            .run(context -> assertThat(context).hasSingleBean(ScheduleClosingScheduler.class));
        schedulerContext().withPropertyValues("spring.profiles.active=test")
            .run(context -> assertThat(context).doesNotHaveBean(ScheduleClosingScheduler.class));
    }

    @Test
    void continuesOtherChallengesAfterFailureAndAdvancesByLastId() {
        ChallengeRepository repository = mock(ChallengeRepository.class);
        ScheduleService schedules = mock(ScheduleService.class);
        ChallengeService challenges = mock(ChallengeService.class);
        ScheduleClosingScheduler scheduler = new ScheduleClosingScheduler(repository, schedules, challenges);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Challenge expired = mock(Challenge.class);
        when(expired.getChallengeId()).thenReturn(10L);
        when(expired.getUserId()).thenReturn(1L);
        when(expired.getEndDate()).thenReturn(today.minusDays(1));
        Challenge running = mock(Challenge.class);
        when(running.getChallengeId()).thenReturn(20L);
        when(running.getUserId()).thenReturn(2L);
        when(running.getEndDate()).thenReturn(today);
        when(repository.findClosingCandidates(eq(today), eq(0L), anyList(), any()))
            .thenReturn(List.of(expired, running));
        when(repository.findClosingCandidates(eq(today), eq(20L), anyList(), any()))
            .thenReturn(List.of());
        when(challenges.settle(1L, 10L)).thenThrow(new IllegalStateException("test failure"));

        scheduler.closeExpiredSchedules();

        verify(challenges).settle(1L, 10L);
        verify(schedules).closeExpiredSchedules(2L, 20L);
        verify(repository).findClosingCandidates(eq(today), eq(20L), anyList(), any());
    }
}
