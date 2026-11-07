package com.moamu.global.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.user.repository.UserRepository;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;

class JwtAuthenticationFilterTests {

  private JwtTokenProvider jwtTokenProvider;
  private UserRepository userRepository;
  private ObjectMapper objectMapper;
  private JwtAuthenticationFilter filter;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {

    // 이전 인증 정보가 테스트에 영향을 주지 않도록 초기화
    SecurityContextHolder.clearContext();

    jwtTokenProvider = mock(JwtTokenProvider.class);
    userRepository = mock(UserRepository.class);
    objectMapper = new ObjectMapper();
    filterChain = mock(FilterChain.class);

    filter = new JwtAuthenticationFilter(
        jwtTokenProvider,
        userRepository,
        objectMapper);
  }

  @AfterEach
  void tearDown() {

    // 현재 테스트의 인증 정보가 다음 테스트에 남지 않도록 정리
    SecurityContextHolder.clearContext();
  }

  // 유효하지 않은 JWT는 401 응답을 반환하고,
  // 회원 조회나 다음 필터 실행으로 넘어가지 않는지 확인
  @Test
  void invalidTokenReturns401() throws Exception {

    // 잘못된 토큰을 포함한 요청 준비
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

    request.addHeader(
        HttpHeaders.AUTHORIZATION,
        "Bearer invalid-token");

    MockHttpServletResponse response = new MockHttpServletResponse();

    // 토큰 검증이 실패하는 상황 설정
    when(jwtTokenProvider.getUserId("invalid-token"))
        .thenThrow(new MalformedJwtException("Invalid JWT"));

    // 실제 인증 필터 실행
    filter.doFilter(request, response, filterChain);

    // HTTP 상태와 공통 오류 응답 확인
    assertEquals(401, response.getStatus());

    JsonNode body = objectMapper.readTree(
        response.getContentAsString());

    assertFalse(body.get("success").asBoolean());
    assertEquals(
        "INVALID_TOKEN",
        body.get("error").get("code").asText());

    // 인증 실패 요청은 회원 조회나 다음 필터로 전달하지 않음
    verifyNoInteractions(userRepository, filterChain);
  }
}