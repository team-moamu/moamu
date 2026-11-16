package com.moamu.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.moamu.global.exception.BusinessException;

@SpringJUnitConfig(EmailVerificationServiceIntegrationTests.TestConfig.class)
@TestPropertySource(properties = {
        "spring.data.redis.host=127.0.0.1",
        "spring.data.redis.port=6379"
})
class EmailVerificationServiceIntegrationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Test
    void correctCodeIssuesTokenAndCannotBeReused() {
        String email = UUID.randomUUID() + "@example.com";
        String code = "003271";
        String codeKey = "auth:signup:code:" + email;

        // 발송 단계와 같은 형태로 테스트용 인증번호를 Redis에 저장한다.
        redisTemplate.opsForValue().set(
                codeKey, passwordEncoder.encode(code), Duration.ofMinutes(5));

        String token = emailVerificationService.verifyCode(email, code);
        assertThat(token).isNotBlank();

        String tokenKey = "auth:signup:token:" + token;

        try {
            assertThat(redisTemplate.opsForValue().get(tokenKey)).isEqualTo(email);
            assertThat(redisTemplate.opsForValue().get(codeKey)).isNull();

            assertThatThrownBy(() -> emailVerificationService.verifyCode(email, code))
                    .isInstanceOf(BusinessException.class);
        } finally {
            redisTemplate.delete(tokenKey);
            redisTemplate.delete(codeKey);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({RedisAutoConfiguration.class, EmailVerificationService.class})
    static class TestConfig {

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }
    }
}