package com.moamu.domain.user.service;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moamu.domain.user.dto.UserLoginRequest;
import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
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

        // 이메일 대소문자 유지해서 중복 확인
        String email = request.getLoginId();
        if (userRepository.existsByLoginId(email)) {
            throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }

        String encodePassword = passwordEncoder.encode(request.getPassword());

        // 가입 시 기본 닉네임을 생성하고, 이후 마이페이지에서 변경
        String nickname = "뮤_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        User user = User.createLocal(email, encodePassword, nickname);
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
        // 입력한 이메일 그대로 중복 조회
        return !userRepository.existsByLoginId(email);
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

    @Override
    public User login(UserLoginRequest request) {

        // 이메일 대소문자를 유지하여 회원 조회
        String email = request.getLoginId();

        User user = userRepository.findByLoginId(email)
                .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_CREDENTIALS));

        // 비밀번호가 없거나 일치하지 않으면 로그인 거절
        if (user.getPassword() == null
                || !passwordEncoder.matches(
                        request.getPassword(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.INVALID_CREDENTIALS);
        }

        // 활성 회원만 로그인 허용
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(UserErrorCode.LOGIN_NOT_ALLOWED);
        }

        return user;
    }

}