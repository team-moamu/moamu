package com.moamu.domain.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.service.UserService;
import com.moamu.global.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTests {

  private UserService userService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    userService = mock(UserService.class);

    mockMvc = MockMvcBuilders
        .standaloneSetup(new UserController(userService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void validSignupReturns201() throws Exception {
    when(userService.signup(any(UserSignupRequest.class)))
        .thenReturn(10L);

    mockMvc.perform(post("/api/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "loginId": "test@example.com",
              "password": "test-password1",
              "name": "테스트회원"
            }
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.userId").value(10))
        .andExpect(jsonPath("$.data.password").doesNotExist())
        .andExpect(jsonPath("$.error").doesNotExist());

    verify(userService).signup(any(UserSignupRequest.class));
  }

  @Test
  void invalidEmailReturns400() throws Exception {
    mockMvc.perform(post("/api/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "loginId": "not-an-email",
              "password": "test-password1",
              "name": "테스트회원"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code")
            .value("INVALID_REQUEST"));

    verifyNoInteractions(userService);
  }

  @Test
  void blankPasswordReturns400() throws Exception {
    mockMvc.perform(post("/api/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "loginId": "test@example.com",
              "password": "",
              "name": "테스트회원"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code")
            .value("INVALID_REQUEST"));

    verifyNoInteractions(userService);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "abc1234",
      "abcdefgh",
      "12345678",
      "abcd 1234",
      "가나다라1234",
      "Abc1234567890123456789"
  })
  void invalidPasswordReturns400(String password) throws Exception {
    mockMvc.perform(post("/api/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "loginId": "test@example.com",
              "password": "%s",
              "name": "테스트회원"
            }
            """.formatted(password)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code")
            .value("INVALID_REQUEST"));

    verifyNoInteractions(userService);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "abcd1234",
      "Abcd123!",
      "Abc12345678901234567"
  })
  void validPasswordReturns201(String password) throws Exception {
    when(userService.signup(any(UserSignupRequest.class)))
        .thenReturn(10L);

    mockMvc.perform(post("/api/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "loginId": "test@example.com",
              "password": "%s",
              "name": "테스트회원"
            }
            """.formatted(password)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.userId").value(10));

    verify(userService).signup(any(UserSignupRequest.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      " test@example.com",
      "test@example.com ",
      "te st@example.com"
  })
  void signupRejectsEmailWithSpaces(String email) throws Exception {
    mockMvc.perform(post("/api/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "loginId": "%s",
              "password": "test-password1",
              "name": "테스트회원"
            }
            """.formatted(email)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code")
            .value("INVALID_REQUEST"));

    verifyNoInteractions(userService);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      " test@example.com",
      "test@example.com ",
      "te st@example.com"
  })
  void availabilityRejectsEmailWithSpaces(String email)
      throws Exception {
    mockMvc.perform(get("/api/users/email-availability")
        .param("email", email))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code")
            .value("INVALID_REQUEST"));

    verifyNoInteractions(userService);
  }
}