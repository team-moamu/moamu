package com.moamu.domain.user.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.domain.user.repository.UserRepository;

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
}