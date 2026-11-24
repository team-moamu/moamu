package com.moamu.domain.artwork.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import com.moamu.domain.artwork.dto.ArtworkGenerationResult;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult.Image;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult.Source;
import com.moamu.domain.artwork.dto.ArtworkGenerationResult.Status;
import com.moamu.domain.artwork.service.GeneratedArtworkValidator.ValidationException;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class GeneratedArtworkValidatorTests {

    private static final String KEY = "generated-artworks/91/2/result.webp";
    private static final Source SOURCE = new Source("catalog-123", "Painting", "https://example.org/work",
            "PUBLIC_DOMAIN", "https://example.org/rights", Instant.parse("2026-01-01T00:00:00Z"));

    @Mock private ObjectProvider<S3Client> clients;
    @Mock private S3Client s3;
    private GeneratedArtworkValidator validator;
    private byte[] content;
    private String hash;

    @BeforeEach
    void setUp() throws Exception {
        content = new byte[9000];
        System.arraycopy("RIFF0000WEBP".getBytes(StandardCharsets.US_ASCII), 0, content, 0, 12);
        hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        validator = new GeneratedArtworkValidator(clients,
                new MockEnvironment().withProperty("app.storage.s3.bucket", "private-artworks"), 10000,
                Duration.ofSeconds(30));
    }

    @Test
    void verifiesTheWholeS3ObjectWithoutAChecksumHeader() {
        stubObject("image/webp", content.length);

        validate(image(KEY, content.length), SOURCE);
    }

    @Test
    void rejectsAChecksumMismatchAtTheEndOfTheFile() {
        content[content.length - 1] = 1;
        stubObject("image/webp", content.length);

        assertInvalid(() -> validate(image(KEY, content.length), SOURCE));
    }

    @Test
    void rejectsAContentTypeMismatch() {
        stubObject("image/png", content.length);

        assertInvalid(() -> validate(image(KEY, content.length), SOURCE));
    }

    @Test
    void rejectsBytesThatDoNotMatchTheDeclaredImageType() {
        content[0] = 0;
        stubObject("image/webp", content.length);

        assertInvalid(() -> validate(image(KEY, content.length), SOURCE));
    }

    @Test
    void rejectsAnActualLengthMismatch() {
        stubObject("image/webp", content.length - 1);

        assertInvalid(() -> validate(image(KEY, content.length), SOURCE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"generated-artworks/91/1/result.webp", "generated-artworks/92/2/result.webp",
            "generated-artworks/91/2/../result.webp"})
    void rejectsAnObjectFromADifferentAttemptBeforeReadingS3(String key) {
        assertInvalid(() -> validate(image(key, content.length), SOURCE));
        verifyNoInteractions(s3);
    }

    @Test
    void rejectsAnOversizedFileOrInvalidSourceBeforeReadingS3() {
        assertInvalid(() -> validate(image(KEY, 10001), SOURCE));
        var invalidSource = new Source(SOURCE.workId(), SOURCE.title(), SOURCE.sourceUrl(),
                "PUBLIC_DOMAIN", "file:///tmp/rights", SOURCE.checkedAt());
        assertInvalid(() -> validate(image(KEY, content.length), invalidSource));
        verifyNoInteractions(s3);
    }

    @Test
    void reportsAS3FailureAsRetryable() {
        when(clients.getIfAvailable()).thenReturn(s3);
        when(s3.getObject(any(GetObjectRequest.class), org.mockito.ArgumentMatchers
                .<ResponseTransformer<GetObjectResponse, Boolean>>any()))
                .thenThrow(S3Exception.builder().statusCode(503).build());

        assertThatThrownBy(() -> validate(image(KEY, content.length), SOURCE))
                .isInstanceOfSatisfying(ValidationException.class, error -> assertThat(error.isRetryable()).isTrue());
    }

    private void stubObject(String contentType, long length) {
        when(clients.getIfAvailable()).thenReturn(s3);
        when(s3.getObject(any(GetObjectRequest.class), org.mockito.ArgumentMatchers
                .<ResponseTransformer<GetObjectResponse, Boolean>>any())).thenAnswer(invocation -> {
                    GetObjectRequest request = invocation.getArgument(0);
                    assertThat(request.bucket()).isEqualTo("private-artworks");
                    assertThat(request.key()).isEqualTo(KEY);
                    ResponseTransformer<GetObjectResponse, Boolean> transformer = invocation.getArgument(1);
                    return transformer.transform(GetObjectResponse.builder().contentType(contentType)
                            .contentLength(length).build(), AbortableInputStream.create(new ByteArrayInputStream(content)));
                });
    }

    private Image image(String key, long length) {
        return new Image(key, "image/webp", length, hash);
    }

    private void validate(Image image, Source source) {
        validator.validate(91L, 2, Instant.now().plusSeconds(30),
                new ArtworkGenerationResult(91L, 2, Status.SUCCEEDED, null, null, image, source, false));
    }

    private void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ValidationException.class,
                error -> assertThat(error.isRetryable()).isFalse());
    }
}
