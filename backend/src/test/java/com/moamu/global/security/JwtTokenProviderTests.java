package com.moamu.global.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moamu.global.config.JwtProperties;

import io.jsonwebtoken.security.SignatureException;

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
        JwtProperties properties = new JwtProperties(secret, 1_800_000L);

        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    // 발급한 토큰의 검증을 통과하면 원래 회원 ID가 반환되는지 확인
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

    // 다른 비밀 키로 서명한 토큰은 서명 검증에 실패하는지 확인한다.
    @Test
    void tokenSignedWithDifferentKeyIsRejected() {

        // 현재 서버와 다른 테스트용 비밀 키 생성
        byte[] otherKeyBytes = new byte[32];
        new SecureRandom().nextBytes(otherKeyBytes);

        String otherSecret = Base64.getEncoder()
                .encodeToString(otherKeyBytes);

        // 다른 키를 사용하는 토큰 발급기 생성
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                new JwtProperties(otherSecret, 1_800_000L));

        // 다른 키로 서명한 토큰 발급
        String token = otherProvider.createAccessToken(10L);

        // 현재 서버의 키로는 서명을 검증할 수 없어야 함
        assertThrows(
                SignatureException.class,
                () -> jwtTokenProvider.getUserId(token));
    }
}