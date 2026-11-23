package com.moamu.domain.artwork.dto;

import java.net.URI;
import java.time.Instant;

// AI가 생성한 작품의 원본 출처와 재사용 허용 여부를 담는 내부 DTO
public record GeneratedArtworkMetadata(
        String workId, String title, URI sourceUrl, String rightsCode, URI rightsUrl,
        Instant checkedAt, Boolean reuseAllowed) {

    public boolean canBeReused() {
        return Boolean.TRUE.equals(reuseAllowed) && hasText(workId) && hasText(title)
                && hasText(rightsCode) && checkedAt != null && isWebUrl(sourceUrl) && isWebUrl(rightsUrl);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isWebUrl(URI uri) {
        return uri != null && uri.getHost() != null && uri.getUserInfo() == null
                && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
    }
}
