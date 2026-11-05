package com.moamu.domain.user.service;

import lombok.RequiredArgsConstructor;

import java.util.Locale;

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

        // 회원가입과 동일하게 이메일을 소문자로 변환하여 조회
        String email = request.getLoginId().toLowerCase(Locale.ROOT);

        // 해당 이메일의 회원이 없으면 로그인 실패
        User user = userRepository.findByLoginId(email)
                .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_CREDENTIALS));

        // 비밀번호가 없는 회원이나 비밀번호가 일치하지 않는 경우 로그인 실패
        if (user.getPassword() == null
                || !passwordEncoder.matches(
                        request.getPassword(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.INVALID_CREDENTIALS);
        }

        // 비밀번호 검증 후 탈퇴 등으로 로그인할 수 없는 회원인지 확인
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(UserErrorCode.LOGIN_NOT_ALLOWED);
        }

        return user;
    }
}