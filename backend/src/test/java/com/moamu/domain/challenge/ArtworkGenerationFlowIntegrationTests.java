package com.moamu.domain.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.moamu.BackendApplicationTests;
import com.moamu.domain.artwork.client.ArtworkGenerationClient;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult;
import com.moamu.domain.artwork.service.ArtworkGenerationWorker;
import com.moamu.domain.challenge.dto.CustomChallengeCreateRequest;
import com.moamu.domain.challenge.service.ChallengeService;
import com.moamu.domain.goal.dto.GoalAnalysisSnapshot;
import com.moamu.domain.goal.dto.GoalEvaluationRequest.CategoryPath;
import com.moamu.domain.goal.repository.GoalAnalysisStore;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@SpringBootTest(properties = {
        "jwt.secret=bW9hbXUtdGVzdC1vbmx5LWtleS0zMi1ieXRlcy1sb25nIQ==",
        "app.storage.s3.bucket=private-artworks"
})
@ActiveProfiles("test")
@Import(BackendApplicationTests.ContainerConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ArtworkGenerationFlowIntegrationTests {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:8.2").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private GoalAnalysisStore analysisStore;
    @Autowired private ChallengeService challengeService;
    @Autowired private ArtworkGenerationWorker worker;
    @MockitoSpyBean private ArtworkGenerationClient ai;
    @MockitoBean private S3Client s3;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void concurrentSubmissionsUseOneGenerationAndConfirmOnlyWithValidImage(boolean validImage) throws Exception {
        long userId = validImage ? 81L : 82L;
        jdbc.update("INSERT INTO users (user_id, name) VALUES (?, 'Artwork flow test')", userId);
        var snapshot = new GoalAnalysisSnapshot(userId, "Walk", "Walk daily", null,
                new CategoryPath("Health", "Exercise", "Walking " + userId),
                List.of("Shoes"), "Show shoes", "Outdoor");
        var start = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        var firstRequest = new CustomChallengeCreateRequest(analysisStore.save(snapshot), start, start.plusDays(6));
        var secondRequest = new CustomChallengeCreateRequest(analysisStore.save(snapshot), start, start.plusDays(7));
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return challengeService.createCustomChallenge(userId, firstRequest);
            });
            var second = executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return challengeService.createCustomChallenge(userId, secondRequest);
            });
            assertThat(first.get(10, TimeUnit.SECONDS).status()).isEqualTo("PENDING");
            assertThat(second.get(10, TimeUnit.SECONDS).status()).isEqualTo("PENDING");
        }

        var firstTask = analysisStore.findArtworkTask(firstRequest.analysisId()).orElseThrow();
        var secondTask = analysisStore.findArtworkTask(secondRequest.analysisId()).orElseThrow();
        long taskId = firstTask.representativeTaskId();
        assertThat(firstTask.taskId()).isNotEqualTo(secondTask.taskId());
        assertThat(secondTask.representativeTaskId()).isEqualTo(taskId);

        byte[] bytes = "RIFF0000WEBP".getBytes(StandardCharsets.US_ASCII);
        String checksum = validImage ? HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                : "0".repeat(64);
        var image = new ArtworkGenerationResult.Image("generated-artworks/" + taskId + "/1/result.webp",
                "image/webp", (long) bytes.length, checksum);
        var source = new ArtworkGenerationResult.Source("work-" + userId, "Painting", "https://example.org/work",
                "PUBLIC_DOMAIN", "https://example.org/rights", Instant.EPOCH);
        doNothing().when(ai).submit(any());
        doReturn(new ArtworkGenerationResult(taskId, 1, ArtworkGenerationResult.Status.SUCCEEDED,
                null, null, image, source, true)).when(ai).getStatus(taskId, 1);
        when(s3.getObject(any(GetObjectRequest.class),
                ArgumentMatchers.<ResponseTransformer<GetObjectResponse, Boolean>>any())).thenAnswer(call -> {
                    ResponseTransformer<GetObjectResponse, Boolean> transformer = call.getArgument(1);
                    return transformer.transform(GetObjectResponse.builder().contentType("image/webp")
                            .contentLength((long) bytes.length).build(),
                            AbortableInputStream.create(new ByteArrayInputStream(bytes)));
                });

        String terminalStatus = validImage ? "SUCCEEDED" : "FAILED_FINAL";
        Instant timeout = Instant.now().plusSeconds(20);
        while (Instant.now().isBefore(timeout)) {
            worker.runNext();
            if (terminalStatus.equals(challengeService.getArtworkTask(userId, firstTask.taskId()).status())
                    && terminalStatus.equals(challengeService.getArtworkTask(userId, secondTask.taskId()).status())) {
                break;
            }
            Thread.sleep(100);
        }
        var body = ArgumentCaptor.forClass(String.class);
        verify(ai).submit(body.capture());
        assertThat(body.getValue()).contains("\"taskId\":" + taskId + ",", "\"taskVersion\":1,");
        verify(s3).getObject(argThat((GetObjectRequest request) ->
                "private-artworks".equals(request.bucket()) && image.key().equals(request.key())),
                ArgumentMatchers.<ResponseTransformer<GetObjectResponse, Boolean>>any());
        assertThat(challengeService.getArtworkTask(userId, firstTask.taskId()).status()).isEqualTo(terminalStatus);
        assertThat(challengeService.getArtworkTask(userId, secondTask.taskId()).status()).isEqualTo(terminalStatus);
        assertThat(challengeCount(userId)).isEqualTo(validImage ? 2 : 0);
        if (validImage) {
            clearInvocations(ai);
            var reuse = new CustomChallengeCreateRequest(analysisStore.save(snapshot), start, start.plusDays(6));
            assertThat(challengeService.createCustomChallenge(userId, reuse).challengeId()).isNotNull();
            assertThat(challengeCount(userId)).isEqualTo(3);
            worker.runNext();
            verifyNoInteractions(ai);
        }
    }

    private int challengeCount(long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM challenge WHERE user_id = ?", Integer.class, userId);
    }
}
