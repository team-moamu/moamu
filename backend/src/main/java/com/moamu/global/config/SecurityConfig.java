package com.moamu.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http) throws Exception {

        http
            // REST API 개발을 위해 CSRF 보호 비활성화
            // JWT 인증 방식 확정 후 재검토
            .csrf(csrf -> csrf.disable())

            // API 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                // JWT 구현 후 인증 권한 설정 필요
                .anyRequest().permitAll()
            )

            // 기본 로그인 화면 비활성화
            .formLogin(form -> form.disable())

            // HTTP Basic 인증 비활성화
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}