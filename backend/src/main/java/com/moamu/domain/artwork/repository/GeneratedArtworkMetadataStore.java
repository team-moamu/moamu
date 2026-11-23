package com.moamu.domain.artwork.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.artwork.dto.GeneratedArtworkMetadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GeneratedArtworkMetadataStore {
    // 작품 ID를 받아 Redis에서 재사용 가능한 작품만 골라내는 저장소 클래스

    private static final String KEY_PREFIX = "artwork:generated:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<Long> findReusableArtworkIds(List<Long> artworkIds) {
        if (artworkIds.isEmpty()) {
            return List.of();
        }
        List<String> keys = artworkIds.stream().map(id -> KEY_PREFIX + id).toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null || values.size() != artworkIds.size()) {
            throw new DataRetrievalFailureException("작품 정보를 조회하지 못했습니다.");
        }

        // 작품 정보가 없거나 읽을 수 없으면 그 작품은 후보에서 뺀다.
        List<Long> reusableArtworkIds = new ArrayList<>();
        for (int index = 0; index < artworkIds.size(); index++) {
            String json = values.get(index);
            if (json == null) {
                continue;
            }
            Long artworkId = artworkIds.get(index);
            try {
                GeneratedArtworkMetadata metadata = objectMapper.readValue(json, GeneratedArtworkMetadata.class);
                if (metadata != null && metadata.canBeReused()) {
                    reusableArtworkIds.add(artworkId);
                }
            } catch (JsonProcessingException exception) {
                log.warn("Invalid generated artwork metadata for artwork {}", artworkId);
            }
        }
        return reusableArtworkIds;
    }
}
