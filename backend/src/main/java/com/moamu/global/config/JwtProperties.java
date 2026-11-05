package com.moamu.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class JwtProperties {

  // Base64로 인코딩된 JWT 서명 키
  private final String secret;

  // 액세스 토큰 유효 기간(밀리초)
  private final long accessTokenExpiration;

  public JwtProperties(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-token-expiration}") long accessTokenExpiration) {

    if (accessTokenExpiration <= 0) {
      throw new IllegalArgumentException(
          "JWT 액세스 토큰 유효 기간은 0보다 커야 합니다.");
    }

    this.secret = secret;
    this.accessTokenExpiration = accessTokenExpiration;
  }
}