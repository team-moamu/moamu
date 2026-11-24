package com.moamu.domain.user.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.domain.user.exception.UserErrorCode;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private final UserRepository userRepository;
  private final EmailVerificationService emailVerificationService;
  private final StringRedisTemplate redisTemplate;
  private final PasswordEncoder passwordEncoder;

  @Async
  public void sendCode(String email) {
    User user = userRepository.findByLoginId(email).orElse(null);

    if (user == null || user.getStatus() != UserStatus.ACTIVE
        || user.getPassword() == null) {
      return;
    }

    emailVerificationService.sendPasswordResetCode(
        user.getLoginId(), user.getUserId());
  }

  public String verifyCode(String email, String code) {
    User user = userRepository.findByLoginId(email).orElse(null);

    if (user == null || user.getStatus() != UserStatus.ACTIVE
        || user.getPassword() == null) {
      throw new BusinessException(UserErrorCode.INVALID_VERIFICATION_CODE);
    }

    return emailVerificationService.verifyPasswordResetCode(
        code, user.getUserId());
  }

  @Transactional
  public void resetPassword(String resetToken, String newPassword) {
    String userId = redisTemplate.opsForValue()
        .getAndDelete("auth:password-reset:token:" + resetToken);

    if (userId == null) {
      throw new BusinessException(UserErrorCode.INVALID_PASSWORD_RESET_TOKEN);
    }

    User user = userRepository.findById(Long.valueOf(userId))
        .filter(member -> member.getStatus() == UserStatus.ACTIVE)
        .filter(member -> member.getPassword() != null)
        .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_PASSWORD_RESET_TOKEN));

    user.changePassword(passwordEncoder.encode(newPassword));
  }
}