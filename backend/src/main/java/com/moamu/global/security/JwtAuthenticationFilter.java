package com.moamu.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.global.response.ApiResponse;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

//요청의 JWT를 검증하고 활성 회원의 인증 정보를 등록
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

    // 토큰이 없는 요청의 접근 가능 여부는 SecurityConfig에서 결정
    if (authorization == null) {
      filterChain.doFilter(request, response);
      return;
    }

    // Bearer 인증 형식이 아닌 요청은 거절
    if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      writeUnauthorized(response);
      return;
    }

    String token = authorization.substring(7).trim();
    Long userId;

    try {
      // 토큰의 서명과 만료 여부를 검증하고 회원 ID 추출
      userId = jwtTokenProvider.getUserId(token);
    } catch (JwtException | IllegalArgumentException exception) {
      writeUnauthorized(response);
      return;
    }

    // 토큰 발급 이후 탈퇴했거나 삭제된 회원도 인증 거절
    User user = userRepository.findById(userId).orElse(null);

    if (user == null || user.getStatus() != UserStatus.ACTIVE) {
      writeUnauthorized(response);
      return;
    }

    // 비밀번호 없이 회원 ID를 현재 요청의 인증 주체로 등록
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        userId, null, List.of());

    SecurityContext context = SecurityContextHolder.createEmptyContext();

    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    // 인증 처리가 끝난 요청을 다음 필터 또는 컨트롤러로 전달
    filterChain.doFilter(request, response);
  }

  // 유효하지 않은 인증 정보에 대해 공통 형식의 401 응답을 반환
  private void writeUnauthorized(HttpServletResponse response)
      throws IOException {

    SecurityContextHolder.clearContext();

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    objectMapper.writeValue(
        response.getWriter(),
        ApiResponse.failure(
            "INVALID_TOKEN",
            "유효하지 않은 인증 정보입니다."));
  }
}