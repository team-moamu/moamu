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
import org.mockito.ArgumentCaptor;
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

  @Test
  void signupSavesEncodedPasswordAndReturnsUserId() {
    UserSignupRequest request = mock(UserSignupRequest.class);

    when(request.getLoginId()).thenReturn("Test@Example.COM");
    when(request.getPassword()).thenReturn("test-password");
    when(request.getName()).thenReturn("테스트회원");

    when(userRepository.existsByLoginId("test@example.com"))
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

    assertEquals("test@example.com", submittedUser.getLoginId());
    assertEquals("테스트회원", submittedUser.getName());
    assertEquals("test-password-hash", submittedUser.getPassword());

    verify(passwordEncoder).encode("test-password");

    verify(userRepository).existsByLoginId("test@example.com");
  }

  @Test
  void emailAvailabilityNormalizesEmail() {
    when(userRepository.existsByLoginId("test@example.com"))
        .thenReturn(false);

    boolean available = userService.isEmailAvailable("Test@Example.COM");

    assertEquals(true, available);

    verify(userRepository).existsByLoginId("test@example.com");
  }

  @Test
  void existingEmailIsUnavailableRegardlessOfCase() {
    when(userRepository.existsByLoginId("test@example.com"))
        .thenReturn(true);

    boolean available = userService.isEmailAvailable("TEST@EXAMPLE.COM");

    assertEquals(false, available);

    verify(userRepository).existsByLoginId("test@example.com");
  }
}
