package com.moamu.domain.goal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.repository.GoalAnalysisStore;
import com.moamu.domain.goal.repository.ArtworkGenerationWorkStore;

@Testcontainers(disabledWithoutDocker = true)
class ArtworkTaskClaimIntegrationTests {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.2"))
            .withExposedPorts(6379);

    @Test
    void concurrentRequestsKeepTheirOwnTasksAndShareOneRepresentative() throws Exception {
        var connection = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connection.afterPropertiesSet();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var redisTemplate = new StringRedisTemplate(connection);
            var objectMapper = new ObjectMapper();
            var store = new GoalAnalysisStore(redisTemplate, objectMapper);
            var workStore = new ArtworkGenerationWorkStore(redisTemplate, objectMapper, store);
            var firstAnalysis = snapshot(1L);
            var secondAnalysis = snapshot(2L);
            String firstId = store.save(firstAnalysis);
            String secondId = store.save(secondAnalysis);
            LocalDate start = LocalDate.of(2026, 11, 23);
            Instant firstAccepted = Instant.now();
            Instant secondAccepted = firstAccepted.plusSeconds(1);
            String firstToken = store.acquireConfirmation(firstId, firstAnalysis, start,
                    start.plusDays(6), firstAccepted).orElseThrow();
            String secondToken = store.acquireConfirmation(secondId, secondAnalysis, start.plusDays(1),
                    start.plusDays(14), secondAccepted).orElseThrow();
            var barrier = new CyclicBarrier(2);
            String categoryKey = UUID.randomUUID().toString();

            var first = executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return store.registerArtworkTask(firstId, firstToken, categoryKey, 90L, 1, firstAccepted);
            });
            var second = executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return store.registerArtworkTask(secondId, secondToken, categoryKey, 91L, 1, secondAccepted);
            });
            var firstTask = first.get(10, TimeUnit.SECONDS);
            var secondTask = second.get(10, TimeUnit.SECONDS);

            assertThat(firstTask.taskId()).isEqualTo(90L);
            assertThat(secondTask.taskId()).isEqualTo(91L);
            assertThat(firstTask.representativeTaskId()).isIn(90L, 91L);
            assertThat(secondTask.representativeTaskId()).isEqualTo(firstTask.representativeTaskId());
            assertThat(firstTask.representativeVersion()).isEqualTo(1);
            assertThat(secondTask.representativeVersion()).isEqualTo(1);
            assertThat(firstTask.expiresAt()).isEqualTo(firstAccepted.plusSeconds(120));
            assertThat(secondTask.expiresAt()).isEqualTo(secondAccepted.plusSeconds(120));
            assertThat(store.findCustomReceipt(secondId).orElseThrow().startDate()).isEqualTo(start.plusDays(1));

            var claimed = workStore.claimNext(Instant.now(), Duration.ofSeconds(10)).orElseThrow();
            assertThat(claimed.taskId()).isEqualTo(firstTask.representativeTaskId());
            assertThat(workStore.claimNext(Instant.now(), Duration.ofSeconds(10))).isEmpty();
            assertThat(workStore.beforeSubmit(claimed, "{\"taskId\":" + claimed.taskId() + "}")).isTrue();
        } finally {
            connection.destroy();
        }
    }

    private GoalAnalysisSnapshot snapshot(long userId) {
        return new GoalAnalysisSnapshot(userId, "Walk", "Walk daily", null,
                new CategoryPath("Health", "Exercise", "Walking"), List.of("Shoes"), "Photograph shoes", "Shoes");
    }
}
