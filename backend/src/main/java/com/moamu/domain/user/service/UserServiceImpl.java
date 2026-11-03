package com.moamu.domain.user.service;

import lombok.RequiredArgsConstructor;

import java.util.Locale;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.exception.UserErrorCode;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.global.exception.BusinessException;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Long signup(UserSignupRequest request) {

        String email = request.getLoginId().toLowerCase(Locale.ROOT);
        if (userRepository.existsByLoginId(email)) {
            throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }

        String encodePassword = passwordEncoder.encode(request.getPassword());

        User user = User.createLocal(email, encodePassword, request.getName());

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return savedUser.getUserId();

        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateLoginId(exception)) {
                throw new BusinessException(
                        UserErrorCode.DUPLICATE_LOGIN_ID);
            }

            throw exception;
        }

    }

    @Override
    public boolean isEmailAvailable(String email) {
        String normalizedEmail = email.toLowerCase(Locale.ROOT);

        return !userRepository.existsByLoginId(normalizedEmail);
    }

    private boolean isDuplicateLoginId(Throwable exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraintName = violation.getConstraintName();

                if (constraintName != null) {
                    String normalizedName = constraintName
                            .replace("`", "")
                            .replace("'", "")
                            .replace("\"", "");

                    if (normalizedName.equalsIgnoreCase("uk_users_login_id")
                            ||
                            normalizedName.toLowerCase(java.util.Locale.ROOT)
                                    .endsWith(".uk_users_login_id")) {
                        return true;
                    }

                }
            }

            cause = cause.getCause();
        }

        return false;
    }

}