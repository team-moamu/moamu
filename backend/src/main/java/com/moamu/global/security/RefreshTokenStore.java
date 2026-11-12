package com.moamu.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

  private static final String KEY_PREFIX = "auth:refresh:";

  private final StringRedisTemplate redisTemplate;

  // 원본 토큰 대신 해시를 키로 사용하고, 만료 시간을 함께 저장합니다.
  public void save(String token, Long userId, Duration ttl) {
    Objects.requireNonNull(userId, "회원 ID가 필요합니다.");
    Objects.requireNonNull(ttl, "유효 기간이 필요합니다.");

    if (userId <= 0 || ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException(
          "회원 ID와 유효 기간은 0보다 커야 합니다.");
    }

    Boolean saved = redisTemplate.opsForValue().setIfAbsent(
        key(token),
        userId.toString(),
        ttl);

    if (!Boolean.TRUE.equals(saved)) {
      throw new IllegalStateException(
          "갱신 토큰을 저장하지 못했습니다.");
    }
  }

  // 조회와 삭제를 한 번에 수행해 같은 토큰의 중복 사용을 막습니다.
  public Optional<Long> consume(String token) {
    String userId = redisTemplate.opsForValue()
        .getAndDelete(key(token));

    return Optional.ofNullable(userId)
        .map(Long::valueOf);
  }

  // 로그아웃할 때 해당 토큰을 폐기합니다.
  public void revoke(String token) {
    redisTemplate.delete(key(token));
  }

  private String key(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException(
          "갱신 토큰이 필요합니다.");
    }

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(
          token.getBytes(StandardCharsets.UTF_8));

      return KEY_PREFIX + HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(
          "토큰 해시를 생성하지 못했습니다.",
          exception);
    }
  }
}