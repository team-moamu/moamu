package com.moamu.domain.verification;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.challenge.entity.Challenge;
import com.moamu.domain.challenge.entity.ChallengeCondition;
import com.moamu.domain.schedule.entity.Schedule;
import com.moamu.domain.schedule.repository.ScheduleRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(BackendApplicationTests.ContainerConfiguration.class)
class VerificationRepositoryTests {

    @Autowired private EntityManager entityManager;
    @Autowired private ScheduleRepository scheduleRepository;

    @ParameterizedTest
    @CsvSource({"2026-11-07,2026-11-07,1", "2026-12-29,2027-01-04,7", "2028-02-28,2028-03-01,3"})
    void joinsConditionsAndComputesInclusiveDaysInOneSelect(LocalDate start, LocalDate end, int days) {
        Challenge challenge = challenge(start, end);
        Schedule schedule = schedule(challenge);
        entityManager.persist(ChallengeCondition.create(challenge.getChallengeId(), "C1", "Book"));
        entityManager.persist(ChallengeCondition.create(challenge.getChallengeId(), "C2", "Date"));
        var statistics = prepareQuery();

        var rows = scheduleRepository.findByChallengeIdAndScheduleId(
                challenge.getChallengeId(), schedule.getScheduleId());

        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.challengeId()).isEqualTo(challenge.getChallengeId());
            assertThat(row.challengeTitle()).isEqualTo("Reading");
            assertThat(row.scheduleId()).isEqualTo(schedule.getScheduleId());
            assertThat(row.dayNo()).isEqualTo(1);
            assertThat(row.dayCount()).isEqualTo(days);
            assertThat(row.attemptCount()).isZero();
        });
        assertThat(rows.stream().flatMap(row -> row.description().stream()).toList())
                .containsExactly("Book", "Date");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void leftJoinKeepsScheduleWithoutConditions() {
        Challenge challenge = challenge(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 13));
        Schedule schedule = schedule(challenge);
        var statistics = prepareQuery();

        var rows = scheduleRepository.findByChallengeIdAndScheduleId(
                challenge.getChallengeId(), schedule.getScheduleId());

        assertThat(rows).singleElement().satisfies(row -> assertThat(row.description()).isEmpty());
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void excludesScheduleBelongingToAnotherChallengeAndMissingIds() {
        LocalDate date = LocalDate.of(2026, 11, 7);
        Challenge first = challenge(date, date);
        Challenge second = challenge(date, date);
        Schedule schedule = schedule(second);
        prepareQuery();

        assertThat(scheduleRepository.findByChallengeIdAndScheduleId(
                first.getChallengeId(), schedule.getScheduleId())).isEmpty();
        assertThat(scheduleRepository.findByChallengeIdAndScheduleId(-1L, schedule.getScheduleId())).isEmpty();
        assertThat(scheduleRepository.findByChallengeIdAndScheduleId(second.getChallengeId(), -1L)).isEmpty();
    }

    private Challenge challenge(LocalDate start, LocalDate end) {
        Challenge challenge = Challenge.create(1L, 1L, 1L, 1L, "Reading", "Read daily", null,
                "Show book", 7, start, end, "Visible book", 1, 7);
        entityManager.persist(challenge);
        return challenge;
    }

    private Schedule schedule(Challenge challenge) {
        Schedule schedule = Schedule.create(challenge.getChallengeId(), 1, challenge.getStartDate(),
                challenge.getStartDate().atStartOfDay(), challenge.getEndDate().atTime(23, 59), 0);
        entityManager.persist(schedule);
        return schedule;
    }

    private org.hibernate.stat.Statistics prepareQuery() {
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
