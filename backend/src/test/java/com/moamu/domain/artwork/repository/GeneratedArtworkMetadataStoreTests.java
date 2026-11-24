package com.moamu.domain.artwork.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class GeneratedArtworkMetadataStoreTests {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> values;

    @Test
    void onlyReturnsArtworksWithCompleteReusableMetadata() {
        String reusable = """
                {"workId":"1","title":"Painting","sourceUrl":"https://example.org/work",
                "rightsCode":"PUBLIC_DOMAIN","rightsUrl":"https://example.org/rights",
                "checkedAt":"2026-11-23T00:00:00Z","reuseAllowed":true}
                """;
        String notReusable = reusable.replace("true", "false");
        String missingRights = reusable.replace("https://example.org/rights", "");
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.multiGet(List.of("artwork:generated:1", "artwork:generated:2", "artwork:generated:3",
                "artwork:generated:4", "artwork:generated:5")))
                .thenReturn(Arrays.asList(reusable, notReusable, missingRights, null, "{"));
        var store = new GeneratedArtworkMetadataStore(redisTemplate,
                new ObjectMapper().registerModule(new JavaTimeModule()));

        assertThat(store.findReusableArtworkIds(List.of(1L, 2L, 3L, 4L, 5L))).containsExactly(1L);
    }
}
