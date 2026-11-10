package com.moamu.global.security;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  // 우선 7일로 설정합니다.
  private static final Duration TOKEN_TTL = Duration.ofDays(7);

  private final SecureRandom secureRandom = new SecureRandom();
  private final RefreshTokenStore refreshTokenStore;

  public IssuedRefreshToken issue(Long userId) {
    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);

    String token = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(randomBytes);

    refreshTokenStore.save(token, userId, TOKEN_TTL);

    return new IssuedRefreshToken(token, TOKEN_TTL);
  }

  public record IssuedRefreshToken(
      String token,
      Duration ttl) {
  }

  public Optional<Long> consume(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    return refreshTokenStore.consume(token);
  }
}