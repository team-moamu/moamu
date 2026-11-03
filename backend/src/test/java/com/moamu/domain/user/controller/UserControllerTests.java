package com.moamu.domain.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.service.UserService;
import com.moamu.global.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
              "password": "test-password",
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
              "password": "test-password",
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
}