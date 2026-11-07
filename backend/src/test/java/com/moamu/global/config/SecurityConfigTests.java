package com.moamu.global.config;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moamu.domain.user.controller.UserController;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.domain.user.service.UserService;
import com.moamu.global.security.JwtTokenProvider;

/**
 * 실제 보안 설정에서 공개 경로와 인증 필요 경로를 구분하는지 확인한다.
 */
@WebMvcTest(UserController.class)
@Import({
    SecurityConfig.class,
    SecurityConfigTests.TestController.class
})
class SecurityConfigTests {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;

  @MockitoBean
  private UserRepository userRepository;

  /**
   * 공개된 이메일 중복 확인 API는 토큰 없이 호출할 수 있는지 확인한다.
   */
  @Test
  void publicEndpointAllowsRequestWithoutToken() throws Exception {

    // 사용 가능한 이메일 조회 결과 설정
    when(userService.isEmailAvailable("Test@example.com"))
        .thenReturn(true);

    // 토큰 없이 공개 API 요청
    mockMvc.perform(get("/api/users/email-availability")
        .param("email", "Test@example.com"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // 인증에서 차단되지 않고 실제 서비스까지 호출됐는지 확인
    verify(userService).isEmailAvailable("Test@example.com");

    // 토큰이 없으므로 JWT 검증 및 인증용 회원 조회는 실행하지 않음
    verifyNoInteractions(jwtTokenProvider, userRepository);
  }

  /**
   * 공개 목록에 없는 경로는 토큰 없이 요청하면 401을 반환하는지 확인한다.
   */
  @Test
  void protectedEndpointRejectsRequestWithoutToken() throws Exception {

    mockMvc.perform(get("/api/security-test"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(
            "WWW-Authenticate", "Bearer"))
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code")
            .value("AUTHENTICATION_REQUIRED"));

    // 인증되지 않은 요청은 업무 처리나 토큰 검증으로 넘어가지 않음
    verifyNoInteractions(
        userService, jwtTokenProvider, userRepository);
  }

  /**
   * 인증된 회원 ID가 컨트롤러까지 전달되는지 확인하는 테스트 전용 API.
   */
  @RestController
  static class TestController {

    @GetMapping("/api/security-test")
    public Long getAuthenticatedUserId(
        @AuthenticationPrincipal Long userId) {

      return userId;
    }
  }

  /**
   * 유효한 JWT를 보낸 활성 회원은 보호된 API에 접근하고,
   * 인증된 회원 ID가 컨트롤러에 전달되는지 확인한다.
   */
  @Test
  void validTokenAllowsAccessToProtectedEndpoint() throws Exception {

    // 토큰에서 추출할 회원 ID 설정
    when(jwtTokenProvider.getUserId("valid-token"))
        .thenReturn(10L);

    // 현재도 활성 상태인 회원 설정
    User user = mock(User.class);
    when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

    when(userRepository.findById(10L))
        .thenReturn(Optional.of(user));

    // 실제 보안 필터 체인을 통과하여 테스트 컨트롤러 호출
    mockMvc.perform(get("/api/security-test")
        .header(
            HttpHeaders.AUTHORIZATION,
            "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(content().string("10"));

    // JWT 검증과 현재 회원 조회가 실행됐는지 확인
    verify(jwtTokenProvider).getUserId("valid-token");
    verify(userRepository).findById(10L);
  }
}