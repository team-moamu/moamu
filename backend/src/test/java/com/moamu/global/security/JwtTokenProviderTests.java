package com.moamu.global.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moamu.global.config.JwtProperties;

//실제 JWT 발급 및 검증 동작을 확인
class JwtTokenProviderTests {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {

        // 외부 환경변수 없이 사용할 테스트 전용 무작위 키 생성
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);

        String secret = Base64.getEncoder()
                .encodeToString(keyBytes);

        // 테스트용 키와 30분의 유효 기간 설정
        JwtProperties properties =
                new JwtProperties(secret, 1_800_000L);

        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    //발급한 토큰의 검증을 통과하면 원래 회원 ID가 반환되는지 확인
    @Test
    void issuedTokenReturnsOriginalUserId() {

        // 토큰을 발급할 회원 ID 준비
        Long userId = 10L;

        // 실제 서명된 액세스 토큰 발급
        String token = jwtTokenProvider.createAccessToken(userId);

        // 실제 서명 및 만료 검증 후 회원 ID 추출
        Long result = jwtTokenProvider.getUserId(token);

        assertFalse(token.isBlank());
        assertEquals(userId, result);
    }
}