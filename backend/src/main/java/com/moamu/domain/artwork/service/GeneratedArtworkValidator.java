package com.moamu.domain.artwork.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.moamu.domain.artwork.dto.ArtworkGenerationResult;
import com.moamu.domain.artwork.dto.GeneratedArtworkMetadata;
import com.moamu.global.util.Sha256;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public class GeneratedArtworkValidator {

    private static final Set<String> CONTENT_TYPES = Set.of("image/webp", "image/png", "image/jpeg");
    private final ObjectProvider<S3Client> s3Clients;
    private final Environment environment;
    private final long maxBytes;
    private final Duration readTimeout;

    public GeneratedArtworkValidator(ObjectProvider<S3Client> s3Clients, Environment environment,
            @Value("${app.artwork-generation.max-image-bytes:10485760}") long maxBytes,
            @Value("${app.ai.read-timeout}") Duration readTimeout) {
        this.s3Clients = s3Clients;
        this.environment = environment;
        this.maxBytes = maxBytes;
        this.readTimeout = readTimeout;
    }

    public void validate(long taskId, int taskVersion, Instant expiresAt, ArtworkGenerationResult result) {
        if (!Objects.equals(result.taskId(), taskId) || !Objects.equals(result.taskVersion(), taskVersion)
                || result.status() != ArtworkGenerationResult.Status.SUCCEEDED || result.reuseAllowed() == null) {
            throw invalid();
        }
        var image = result.image();
        String prefix = "generated-artworks/" + taskId + "/" + taskVersion + "/";
        if (image == null || image.key() == null || image.key().length() > 500 || !image.key().startsWith(prefix)
                || !image.key().substring(prefix.length()).matches("[A-Za-z0-9][A-Za-z0-9._-]*")
                || image.sizeBytes() == null || image.sizeBytes() <= 0 || image.sizeBytes() > maxBytes
                || image.contentType() == null || !CONTENT_TYPES.contains(image.contentType())
                || image.sha256() == null || !image.sha256().matches("[0-9a-fA-F]{64}")) {
            throw invalid();
        }
        validateSource(result);
        Duration remaining = remainingTime(expiresAt);
        S3Client s3 = s3Clients.getIfAvailable();
        if (s3 == null) {
            throw new ValidationException(true, null);
        }
        String bucket = environment.getProperty("app.storage.s3.bucket");
        if (bucket == null || bucket.isBlank()) {
            throw new ValidationException(true, null);
        }
        Duration timeout = remaining.compareTo(readTimeout) < 0 ? remaining : readTimeout;
        var request = GetObjectRequest.builder().bucket(bucket.strip()).key(image.key())
                .overrideConfiguration(config -> config.apiCallTimeout(timeout)).build();
        // 체크섬 메타데이터가 없어도 실제 파일을 한 번 읽으면서 크기와 해시를 확인한다.
        ResponseTransformer<GetObjectResponse, Boolean> verifyObject = (response, body) -> {
            boolean verified = false;
            try {
                verified = matchesObject(image, response, body, expiresAt);
                return verified;
            } finally {
                if (!verified) {
                    body.abort();
                }
            }
        };
        try {
            if (!Boolean.TRUE.equals(s3.getObject(request, verifyObject))) {
                throw invalid();
            }
        } catch (SdkException exception) {
            throw new ValidationException(true, exception);
        }
        remainingTime(expiresAt);
    }

    private void validateSource(ArtworkGenerationResult result) {
        var source = result.source();
        if (source == null || !"PUBLIC_DOMAIN".equals(source.rightsCode())) {
            throw invalid();
        }
        try {
            var metadata = new GeneratedArtworkMetadata(source.workId(), source.title(),
                    URI.create(source.sourceUrl() == null ? "" : source.sourceUrl()), source.rightsCode(),
                    URI.create(source.rightsUrl() == null ? "" : source.rightsUrl()), source.checkedAt(),
                    result.reuseAllowed());
            if (!metadata.hasValidSource() || source.checkedAt().isAfter(Instant.now())) {
                throw invalid();
            }
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private boolean matchesObject(ArtworkGenerationResult.Image image, GetObjectResponse response,
            InputStream body, Instant expiresAt) throws IOException {
        if (!image.sizeBytes().equals(response.contentLength())
                || !image.contentType().equals(response.contentType())) {
            return false;
        }
        byte[] header = body.readNBytes((int) Math.min(12, image.sizeBytes()));
        if (!matchesImageType(image.contentType(), header)) {
            return false;
        }
        MessageDigest digest = Sha256.newDigest();
        digest.update(header);
        long size = header.length;
        byte[] buffer = new byte[8192];
        while (size < image.sizeBytes()) {
            remainingTime(expiresAt);
            int count = body.read(buffer, 0, (int) Math.min(buffer.length, image.sizeBytes() - size));
            if (count < 0) {
                return false;
            }
            digest.update(buffer, 0, count);
            size += count;
        }
        remainingTime(expiresAt);
        return body.read() == -1 && MessageDigest.isEqual(digest.digest(), HexFormat.of().parseHex(image.sha256()));
    }

    private boolean matchesImageType(String type, byte[] header) {
        return switch (type) {
            case "image/jpeg" -> header.length >= 3
                    && header[0] == (byte) 0xff && header[1] == (byte) 0xd8 && header[2] == (byte) 0xff;
            case "image/png" -> header.length >= 8 && HexFormat.of().formatHex(header, 0, 8).equals("89504e470d0a1a0a");
            case "image/webp" ->
                header.length == 12 && new String(header, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                        && new String(header, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
    }

    private Duration remainingTime(Instant expiresAt) {
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (remaining.isNegative() || remaining.isZero()) {
            throw new ValidationException(true, null);
        }
        return remaining;
    }

    private ValidationException invalid() {
        return new ValidationException(false, null);
    }

    public static class ValidationException extends RuntimeException {
        private final boolean retryable;

        ValidationException(boolean retryable, Throwable cause) {
            super("명화 생성 결과를 검증하지 못했습니다.", cause);
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }
}
