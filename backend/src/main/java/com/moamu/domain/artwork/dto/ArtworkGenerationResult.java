package com.moamu.domain.artwork.dto;

import java.time.Instant;

public record ArtworkGenerationResult(
        Long taskId,
        Integer taskVersion,
        Status status,
        String errorCode,
        Boolean retryable,
        Image image,
        Source source,
        Boolean reuseAllowed) {

    public enum Status {
        PENDING, RUNNING, FAILED, SUCCEEDED
    }

    public record Image(String key, String contentType, Long sizeBytes, String sha256) {
    }

    public record Source(String workId, String title, String sourceUrl, String rightsCode, String rightsUrl,
            Instant checkedAt) {
    }
}
