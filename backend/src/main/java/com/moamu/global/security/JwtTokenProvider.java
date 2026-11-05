package com.moamu.global.security;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.moamu.global.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

// 로그인에 사용할 토큰 발급
@Component
public class JwtTokenProvider {

  private final SecretKey secretKey;
  private final long accessTokenExpiration;

  public JwtTokenProvider(JwtProperties properties) {

    // Base64 문자열을 원래 키 바이트로 복원하여 서명용 키 생성
    this.secretKey = Keys.hmacShaKeyFor(
        Decoders.BASE64.decode(properties.getSecret()));

    this.accessTokenExpiration = properties.getAccessTokenExpiration();
  }

  public String createAccessToken(Long userId) {

    // 발급 시각을 기준으로 만료 시각 계산
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plusMillis(accessTokenExpiration);

    return Jwts.builder()
        .subject(userId.toString())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey, Jwts.SIG.HS256)
        .compact();
  }

  public Long getUserId(String token) {

    // 서명 및 만료 검증을 통과한 토큰의 내용만 사용
    Claims claims = Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();

    String subject = claims.getSubject();

    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException(
          "토큰에 회원 ID가 없습니다.");
    }

    return Long.valueOf(subject);
  }
}