package com.moamu.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.moamu.domain.user.dto.UserLoginRequest;
import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.domain.user.exception.UserErrorCode;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.global.exception.BusinessException;

public class UserServiceImplTests {

        private UserRepository userRepository;
        private PasswordEncoder passwordEncoder;
        private UserServiceImpl userService;

        @BeforeEach
        void setUp() {
                userRepository = mock(UserRepository.class);
                passwordEncoder = mock(PasswordEncoder.class);

                userService = new UserServiceImpl(userRepository, passwordEncoder);

        }

        @Test
        void duplicateConstraintBecomesBusinessException() {
                UserSignupRequest request = mock(UserSignupRequest.class);

                when(request.getLoginId()).thenReturn("test@example.com");
                when(request.getPassword()).thenReturn("test-password");

                when(userRepository.existsByLoginId("test@example.com")).thenReturn(false);

                when(passwordEncoder.encode("test-password")).thenReturn("test-password-hash");

                ConstraintViolationException constraintException = new ConstraintViolationException(
                                "duplicate login ID",
                                new SQLException("duplicate", "23000", 1062),
                                "users.uk_users_login_id");

                DataIntegrityViolationException databaseException = new DataIntegrityViolationException(
                                "save failed",
                                constraintException);

                when(userRepository.saveAndFlush(any(User.class)))
                                .thenThrow(databaseException);

                BusinessException result = assertThrows(
                                BusinessException.class,
                                () -> userService.signup(request));

                assertEquals(
                                UserErrorCode.DUPLICATE_LOGIN_ID,
                                result.getErrorCode());

                verify(userRepository).saveAndFlush(any(User.class));

        }

        @Test
        void otherConstraintIsNotConvertedToDuplicateEmail() {
                UserSignupRequest request = mock(UserSignupRequest.class);

                when(request.getLoginId()).thenReturn("test@example.com");
                when(request.getPassword()).thenReturn("test-password");

                when(userRepository.existsByLoginId("test@example.com"))
                                .thenReturn(false);

                when(passwordEncoder.encode("test-password"))
                                .thenReturn("test-password-hash");

                ConstraintViolationException constraintException = new ConstraintViolationException(
                                "foreign key violation",
                                new SQLException("foreign key violation", "23000", 1452),
                                "FK_media_TO_users_1");

                DataIntegrityViolationException databaseException = new DataIntegrityViolationException(
                                "save failed",
                                constraintException);

                when(userRepository.saveAndFlush(any(User.class)))
                                .thenThrow(databaseException);

                DataIntegrityViolationException result = assertThrows(
                                DataIntegrityViolationException.class,
                                () -> userService.signup(request));

                assertSame(databaseException, result);
        }

        @Test
        void existingEmailIsRejectedBeforeSaving() {
                UserSignupRequest request = mock(UserSignupRequest.class);

                when(request.getLoginId()).thenReturn("test@example.com");

                when(userRepository.existsByLoginId("test@example.com"))
                                .thenReturn(true);

                BusinessException result = assertThrows(
                                BusinessException.class,
                                () -> userService.signup(request));

                assertEquals(
                                UserErrorCode.DUPLICATE_LOGIN_ID,
                                result.getErrorCode());

                verifyNoInteractions(passwordEncoder);

                verify(userRepository, never())
                                .saveAndFlush(any(User.class));
        }

        @Test
        void signupSavesEncodedPasswordAndReturnsUserId() {
                UserSignupRequest request = mock(UserSignupRequest.class);

                when(request.getLoginId()).thenReturn("Test@Example.COM");
                when(request.getPassword()).thenReturn("test-password");

                when(userRepository.existsByLoginId("Test@Example.COM"))
                                .thenReturn(false);

                when(passwordEncoder.encode("test-password"))
                                .thenReturn("test-password-hash");

                User savedUser = mock(User.class);
                when(savedUser.getUserId()).thenReturn(10L);

                when(userRepository.saveAndFlush(any(User.class)))
                                .thenReturn(savedUser);

                Long userId = userService.signup(request);

                assertEquals(Long.valueOf(10L), userId);

                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

                verify(userRepository).saveAndFlush(captor.capture());

                User submittedUser = captor.getValue();

                assertEquals("Test@Example.COM", submittedUser.getLoginId());
                assertTrue(submittedUser.getName().matches("^뮤_[0-9a-f]{12}$"));
                assertEquals("test-password-hash", submittedUser.getPassword());

                verify(passwordEncoder).encode("test-password");

                verify(userRepository).existsByLoginId("Test@Example.COM");
        }

        @Test
        void emailAvailabilityPreservesCase() {
                when(userRepository.existsByLoginId("Test@Example.COM"))
                                .thenReturn(false);

                boolean available = userService.isEmailAvailable("Test@Example.COM");

                assertEquals(true, available);

                verify(userRepository).existsByLoginId("Test@Example.COM");
        }

        @Test
        void sameCaseExistingEmailIsUnavailable() {
                when(userRepository.existsByLoginId("TEST@EXAMPLE.COM"))
                                .thenReturn(true);

                boolean available = userService.isEmailAvailable("TEST@EXAMPLE.COM");

                assertEquals(false, available);

                verify(userRepository).existsByLoginId("TEST@EXAMPLE.COM");
        }

        @Test
        void loginReturnsActiveUserWhenCredentialsMatch() {

                // 로그인 요청 정보 준비
                UserLoginRequest request = mock(UserLoginRequest.class);

                when(request.getLoginId()).thenReturn("Test@example.com");
                when(request.getPassword()).thenReturn("Password123!");

                // 조회할 활성 회원 준비
                User user = mock(User.class);

                when(user.getPassword()).thenReturn("stored-password-hash");
                when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

                // 입력한 이메일로 회원이 조회되도록 설정
                when(userRepository.findByLoginId("Test@example.com"))
                                .thenReturn(Optional.of(user));

                // 입력 비밀번호와 저장된 해시가 일치하도록 설정
                when(passwordEncoder.matches(
                                "Password123!", "stored-password-hash"))
                                .thenReturn(true);

                // 로그인 검증 실행
                User result = userService.login(request);

                // 검증을 통과한 회원이 반환되는지 확인
                assertSame(user, result);

                // 이메일 대소문자를 유지하여 조회했는지 확인
                verify(userRepository).findByLoginId("Test@example.com");

                // 원본 비밀번호와 저장된 해시로 비교했는지 확인
                verify(passwordEncoder).matches(
                                "Password123!", "stored-password-hash");
        }

        // 비밀번호가 일치하지 않으면 인증 실패 예외를 발생시키고, 회원 상태 확인까지 진행하지 않는지 검증
        @Test
        void loginRejectsIncorrectPassword() {

                // 로그인 요청 정보 준비
                UserLoginRequest request = mock(UserLoginRequest.class);

                when(request.getLoginId()).thenReturn("Test@example.com");
                when(request.getPassword()).thenReturn("WrongPassword123!");

                // 조회된 회원의 저장된 비밀번호 해시 설정
                User user = mock(User.class);

                when(user.getPassword()).thenReturn("stored-password-hash");

                when(userRepository.findByLoginId("Test@example.com"))
                                .thenReturn(Optional.of(user));

                // 입력 비밀번호가 저장된 해시와 일치하지 않는 상황 설정
                when(passwordEncoder.matches(
                                "WrongPassword123!", "stored-password-hash"))
                                .thenReturn(false);

                // 비밀번호 불일치 시 인증 실패 예외가 발생하는지 확인
                BusinessException exception = assertThrows(
                                BusinessException.class,
                                () -> userService.login(request));

                assertEquals(
                                UserErrorCode.INVALID_CREDENTIALS,
                                exception.getErrorCode());

                // 비밀번호 검증에서 실패하면 회원 상태를 확인하지 않음
                verify(user, never()).getStatus();
        }

        /**
         * 등록되지 않은 이메일로 로그인하면 인증 실패 예외가 발생하고,
         * 비밀번호 검증은 실행하지 않는지 확인한다.
         */
        @Test
        void loginRejectsUnregisteredEmail() {

                // 등록되지 않은 이메일로 로그인 요청 준비
                UserLoginRequest request = mock(UserLoginRequest.class);

                when(request.getLoginId()).thenReturn("unknown@example.com");

                // 해당 이메일의 회원이 없는 상황 설정
                when(userRepository.findByLoginId("unknown@example.com"))
                                .thenReturn(Optional.empty());

                // 회원이 없으면 인증 실패 예외가 발생하는지 확인
                BusinessException exception = assertThrows(
                                BusinessException.class,
                                () -> userService.login(request));

                assertEquals(
                                UserErrorCode.INVALID_CREDENTIALS,
                                exception.getErrorCode());

                // 회원 조회에서 실패하면 비밀번호 검증을 진행하지 않음
                verifyNoInteractions(passwordEncoder);
        }

        /**
         * 저장된 비밀번호가 없는 회원은 로그인을 거절하고,
         * 비밀번호 비교를 실행하지 않는지 확인한다.
         */
        @Test
        void loginRejectsUserWithoutPassword() {

                // 로그인 요청 정보 준비
                UserLoginRequest request = mock(UserLoginRequest.class);

                when(request.getLoginId()).thenReturn("social@example.com");

                // 저장된 비밀번호가 없는 회원 설정
                User user = mock(User.class);
                when(user.getPassword()).thenReturn(null);

                when(userRepository.findByLoginId("social@example.com"))
                                .thenReturn(Optional.of(user));

                // 비밀번호가 없는 회원은 인증 실패 처리
                BusinessException exception = assertThrows(
                                BusinessException.class,
                                () -> userService.login(request));

                assertEquals(
                                UserErrorCode.INVALID_CREDENTIALS,
                                exception.getErrorCode());

                // 비밀번호 비교와 회원 상태 확인까지 진행하지 않음
                verifyNoInteractions(passwordEncoder);
                verify(user, never()).getStatus();
        }

        /**
         * 이메일과 비밀번호가 일치해도 탈퇴 회원이면
         * 로그인 불가 예외가 발생하는지 확인한다.
         */
        @Test
        void loginRejectsWithdrawnUser() {

                // 로그인 요청 정보 준비
                UserLoginRequest request = mock(UserLoginRequest.class);

                when(request.getLoginId()).thenReturn("Test@example.com");
                when(request.getPassword()).thenReturn("Password123!");

                // 비밀번호가 등록된 탈퇴 회원 설정
                User user = mock(User.class);

                when(user.getPassword()).thenReturn("stored-password-hash");
                when(user.getStatus()).thenReturn(UserStatus.WITHDRAWN);

                when(userRepository.findByLoginId("Test@example.com"))
                                .thenReturn(Optional.of(user));

                // 비밀번호 검증은 통과하도록 설정
                when(passwordEncoder.matches(
                                "Password123!", "stored-password-hash"))
                                .thenReturn(true);

                // 회원 상태 검증에서 로그인이 거절되는지 확인
                BusinessException exception = assertThrows(
                                BusinessException.class,
                                () -> userService.login(request));

                assertEquals(
                                UserErrorCode.LOGIN_NOT_ALLOWED,
                                exception.getErrorCode());

                // 비밀번호 검증과 회원 상태 확인이 실행됐는지 확인
                verify(passwordEncoder).matches(
                                "Password123!", "stored-password-hash");
                verify(user).getStatus();
        }
}
