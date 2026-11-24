package com.moamu.domain.user.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

  @Async
  public void sendCode(String email) {
    User user = userRepository.findByLoginId(email).orElse(null);

    if (user == null || user.getStatus() != UserStatus.ACTIVE
        || user.getPassword() == null) {
      return;
    }

    emailVerificationService.sendPasswordResetCode(user.getLoginId());
  }

  public String verifyCode(String email, String code) {
    User user = userRepository.findByLoginId(email).orElse(null);

    if (user == null || user.getStatus() != UserStatus.ACTIVE
        || user.getPassword() == null) {
      throw new BusinessException(UserErrorCode.INVALID_VERIFICATION_CODE);
    }

    return emailVerificationService.verifyPasswordResetCode(
        user.getLoginId(), code, user.getUserId());
  }
}