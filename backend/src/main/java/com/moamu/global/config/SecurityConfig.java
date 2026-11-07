package com.moamu.global.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.global.response.ApiResponse;
import com.moamu.global.security.JwtAuthenticationFilter;
import com.moamu.global.security.JwtTokenProvider;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

//JWT 인증 필터, API 접근 권한 및 인증 실패 응답을 설정
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            ObjectMapper objectMapper) throws Exception {

        // 서블릿 자동 등록 없이 보안 필터 체인에서만 사용할 필터 생성
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                userRepository,
                objectMapper);

        http
                // 쿠키 대신 Authorization 헤더의 JWT를 사용하는 인증 방식
                .csrf(csrf -> csrf.disable())

                // 로그인 상태를 HTTP 세션에 저장하지 않음
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                // 세션 기반 요청 저장 기능 비활성화
                .requestCache(cache -> cache.disable())

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())

                // 로그인 없이 호출할 수 있는 경로와 보호할 경로 설정
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/signup",
                                "/api/users/login")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/email-availability")
                        .permitAll()

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()

                        .anyRequest().authenticated())

                // 인증 또는 접근 권한이 부족한 경우 공통 JSON 응답 반환
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, authException) -> writeSecurityError(
                                        response,
                                        objectMapper,
                                        401,
                                        "AUTHENTICATION_REQUIRED",
                                        "로그인이 필요합니다."))
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> writeSecurityError(
                                        response,
                                        objectMapper,
                                        403,
                                        "ACCESS_DENIED",
                                        "접근 권한이 없습니다.")))

                // 인가 판단에 앞서 JWT 인증 정보를 등록
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 보안 처리 중 발생한 오류를 공통 API 응답 형식으로 반환
    private void writeSecurityError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int status,
            String code,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }

        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.failure(code, message));
    }
}