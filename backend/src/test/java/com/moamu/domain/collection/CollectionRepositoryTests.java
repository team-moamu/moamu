package com.moamu.domain.collection;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.collection.repository.CollectionRepository;
import com.moamu.domain.schedule.entity.Schedule;
import com.moamu.domain.schedule.enums.Status;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(BackendApplicationTests.ContainerConfiguration.class)
class CollectionRepositoryTests {
    @Autowired EntityManager em;
    @Autowired CollectionRepository repository;

    @Test
    void sortsByRatioNotSuccessCountAndKeepsZeroSuccessCollections() {
        Challenge high = challenge("High", 7, 5, 'Y', 1L, 1);
        Challenge low = challenge("Low", 30, 20, 'N', 1L, 2);
        Challenge zero = challenge("Zero", 7, 0, 'N', 1L, 3);
        Challenge invalidDuration = challenge("Zero duration", 0, 0, 'N', 1L, 4);
        challenge("Other owner", 7, 7, 'Y', 2L, 5);
        flush();

        assertThat(repository.findOwned(1L, null, null, "successRate"))
            .extracting(Challenge::getChallengeId)
            .containsExactly(high.getChallengeId(), low.getChallengeId(),
                invalidDuration.getChallengeId(), zero.getChallengeId());
        assertThat(repository.findOwned(1L, 'Y', null, "successRate"))
            .extracting(Challenge::getChallengeId).containsExactly(high.getChallengeId());
        assertThat(repository.findOwned(1L, 'N', null, "successRate"))
            .extracting(Challenge::getChallengeId).containsExactly(low.getChallengeId(),
                invalidDuration.getChallengeId(), zero.getChallengeId());
    }

    @Test
    void equalRatesUseDescendingId() {
        Challenge first = challenge("First", 7, 5, 'N', 1L, 1);
        Challenge second = challenge("Second", 14, 10, 'N', 1L, 2);
        flush();
        assertThat(repository.findOwned(1L, null, null, "successRate"))
            .extracting(Challenge::getChallengeId)
            .containsExactly(second.getChallengeId(), first.getChallengeId());
    }

    @Test
    void preservesLatestAndNameSorts() {
        Challenge a = challenge("A", 7, 1, 'N', 1L, 2);
        Challenge b = challenge("B", 7, 7, 'N', 1L, 1);
        flush();
        assertThat(repository.findOwned(1L, null, null, "latest"))
            .extracting(Challenge::getChallengeId).containsExactly(a.getChallengeId(), b.getChallengeId());
        assertThat(repository.findOwned(1L, null, null, "name"))
            .extracting(Challenge::getChallengeId).containsExactly(a.getChallengeId(), b.getChallengeId());
    }

    @Test
    void searchesChallengeTitleByPartialKeywordAndCombinesExhibitionFilter() {
        Challenge pearl = challenge("공부하는 진주귀걸이를 한 소녀", 7, 5, 'Y', 1L, 1);
        challenge("진주를 찾는 도전", 7, 4, 'N', 1L, 2);
        challenge("명찰 챙긴 모나리자", 7, 3, 'Y', 1L, 3);
        flush();

        assertThat(repository.findOwned(1L, null, "진주", "latest"))
            .extracting(Challenge::getChallengeTitle)
            .containsExactly("진주를 찾는 도전", "공부하는 진주귀걸이를 한 소녀");

        assertThat(repository.findOwned(1L, 'Y', "진주", "latest"))
            .extracting(Challenge::getChallengeId)
            .containsExactly(pearl.getChallengeId());

        assertThat(repository.findOwned(1L, null, "없는 이름", "latest")).isEmpty();
    }

    private Challenge challenge(String title, int days, int successes, char exhibited, long userId, int endDay) {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 11, endDay);
        Challenge c = Challenge.create(userId, 1L, 1L, 1L, title, "Goal", null,
            "Guide", days, start, end, "Scope", 1, 7);
        c.startIfDue(start);
        c.settle(true);
        c.setIsExhibited(exhibited);
        em.persist(c);
        for (int i = 0; i < days; i++) {
            Schedule s = Schedule.create(c.getChallengeId(), i + 1, start.plusDays(i),
                start.atStartOfDay(), end.atStartOfDay(), i);
            s.completeVerification(i < successes ? Status.SUCCESS : Status.FAILED, null);
            em.persist(s);
        }
        return c;
    }

    private void flush() {
        em.flush();
        em.clear();
    }
}
