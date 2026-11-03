package com.moamu.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.entity.User;
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
    when(request.getName()).thenReturn("테스트회원");

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
    when(request.getName()).thenReturn("테스트회원");

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
}
