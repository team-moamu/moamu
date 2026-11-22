package com.moamu.domain.gallery;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.gallery.repository.GalleryRepository;
import com.moamu.domain.schedule.entity.Schedule;
import com.moamu.domain.schedule.enums.Status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(BackendApplicationTests.ContainerConfiguration.class)
class GalleryRepositoryTests {

    @Autowired EntityManager em;
    @Autowired GalleryRepository repository;

    @Test
    void hidingPreservesChallengeAndEnforcesOwnershipAndDeletionFilter() {
        Challenge exhibited = challenge("Exhibited", 7, 5, 'Y', 1L, 1);
        Long id = exhibited.getChallengeId();
        flush();
        assertThat(repository.findOwnedForExhibition(2L, id)).isEmpty();
        repository.findOwnedForExhibition(1L, id).orElseThrow().setIsExhibited('N');
        flush();
        assertThat(repository.findExhibited(1L, "endDate", "desc")).isEmpty();
        assertThat(repository.findOwnedForExhibition(1L, id)).isPresent();
        assertThat(repository.findById(id).orElseThrow().getIsExhibited()).isEqualTo('N');
        em.createQuery("UPDATE Challenge c SET c.delYn = 'Y' WHERE c.challengeId = :id")
            .setParameter("id", id).executeUpdate();
        em.clear();
        assertThat(repository.findOwnedForExhibition(1L, id)).isEmpty();
    }

    @Test
    void returnsOnlyCompletedExhibitedChallengesOwnedByUser() {
        Challenge exhibited = challenge("Exhibited", 7, 5, 'Y', 1L, 1);
        challenge("Not exhibited", 7, 7, 'N', 1L, 2);
        challenge("Other owner", 7, 7, 'Y', 2L, 3);
        flush();

        assertThat(repository.findExhibited(1L, "endDate", "desc"))
            .extracting(Challenge::getChallengeId)
            .containsExactly(exhibited.getChallengeId());
    }

    @Test
    void supportsLatestNameAndSuccessRateSorts() {
        Challenge alpha = challenge("Alpha", 7, 5, 'Y', 1L, 2);
        Challenge beta = challenge("Beta", 30, 20, 'Y', 1L, 3);
        Challenge gamma = challenge("Gamma", 7, 0, 'Y', 1L, 1);
        flush();

        assertThat(repository.findExhibited(1L, "endDate", "desc"))
            .extracting(Challenge::getChallengeId)
            .containsExactly(beta.getChallengeId(), alpha.getChallengeId(), gamma.getChallengeId());

        assertThat(repository.findExhibited(1L, "name", "asc"))
            .extracting(Challenge::getChallengeId)
            .containsExactly(alpha.getChallengeId(), beta.getChallengeId(), gamma.getChallengeId());

        assertThat(repository.findExhibited(1L, "successRate", "desc"))
            .extracting(Challenge::getChallengeId)
            .containsExactly(alpha.getChallengeId(), beta.getChallengeId(), gamma.getChallengeId());
    }

    @Test
    void supportsReverseDirectionsAndStableTies() {
        Challenge alpha = challenge("Alpha", 7, 5, 'Y', 1L, 2);
        Challenge beta = challenge("Beta", 30, 20, 'Y', 1L, 3);
        Challenge gamma = challenge("Gamma", 7, 0, 'Y', 1L, 1);
        flush();
        assertThat(repository.findExhibited(1L, "endDate", "asc"))
            .extracting(Challenge::getChallengeId)
            .containsExactly(gamma.getChallengeId(), alpha.getChallengeId(), beta.getChallengeId());
        assertThat(repository.findExhibited(1L, "name", "desc"))
            .extracting(Challenge::getChallengeId)
            .containsExactly(gamma.getChallengeId(), beta.getChallengeId(), alpha.getChallengeId());
        assertThat(repository.findExhibited(1L, "successRate", "asc"))
            .extracting(Challenge::getChallengeId)
            .containsExactly(gamma.getChallengeId(), beta.getChallengeId(), alpha.getChallengeId());
        Challenge tied = challenge("Alpha", 7, 5, 'Y', 1L, 2);
        flush();
        assertThat(repository.findExhibited(1L, "name", "asc"))
            .extracting(Challenge::getChallengeId)
            .containsExactly(tied.getChallengeId(), alpha.getChallengeId(), beta.getChallengeId(), gamma.getChallengeId());
    }

    private Challenge challenge(
        String title,
        int durationDays,
        int successDays,
        char exhibited,
        long userId,
        int endDay
    ) {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 11, endDay);
        Challenge challenge = Challenge.create(
            userId,
            1L,
            1L,
            1L,
            title,
            "Goal",
            null,
            "Guide",
            durationDays,
            startDate,
            endDate,
            "Scope",
            1,
            7
        );
        challenge.startIfDue(startDate);
        challenge.settle(true);
        challenge.setIsExhibited(exhibited);
        em.persist(challenge);

        for (int index = 0; index < durationDays; index++) {
            Schedule schedule = Schedule.create(
                challenge.getChallengeId(),
                index + 1,
                startDate.plusDays(index),
                startDate.atStartOfDay(),
                endDate.atStartOfDay(),
                index
            );
            schedule.completeVerification(
                index < successDays ? Status.SUCCESS : Status.FAILED,
                null
            );
            em.persist(schedule);
        }

        return challenge;
    }

    private void flush() {
        em.flush();
        em.clear();
    }
}
