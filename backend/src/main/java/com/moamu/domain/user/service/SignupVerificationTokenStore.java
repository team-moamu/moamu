package com.moamu.domain.user.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SignupVerificationTokenStore {

    private final StringRedisTemplate redisTemplate;

    public boolean matchesEmail(String token, String email) {
        String verifiedEmail = redisTemplate.opsForValue().get(key(token));
        return email.equals(verifiedEmail);
    }

    public void delete(String token) {
        redisTemplate.delete(key(token));
    }

    static String key(String token) {
        return "auth:signup:token:" + token;
    }
}