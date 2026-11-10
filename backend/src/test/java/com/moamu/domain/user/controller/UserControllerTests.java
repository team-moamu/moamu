package com.moamu.domain.user.controller;

import org.junit.jupiter.api.BeforeEach;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.moamu.domain.user.dto.UserLoginRequest;
import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.exception.UserErrorCode;
import com.moamu.domain.user.service.UserService;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.GlobalExceptionHandler;
import com.moamu.global.security.JwtTokenProvider;
import com.moamu.global.security.RefreshTokenService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTests {

    private UserService userService;
    private JwtTokenProvider jwtTokenProvider;
    private MockMvc mockMvc;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        refreshTokenService = mock(RefreshTokenService.class);

        UserController controller = new UserController(
                userService,
                jwtTokenProvider,
                refreshTokenService);

        // standalone 테스트에서는 @Value가 자동 주입되지 않습니다.
        ReflectionTestUtils.setField(
                controller, "refreshCookieSecure", true);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
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

    @Test
    void validLoginReturnsAccessToken() throws Exception {

        // 로그인 검증을 통과한 회원 준비
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(10L);

        when(userService.login(any(UserLoginRequest.class)))
                .thenReturn(user);

        // 검증된 회원에게 발급할 토큰 설정
        when(jwtTokenProvider.createAccessToken(10L))
                .thenReturn("test-access-token");

        when(refreshTokenService.issue(10L))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken(
                        "test-refresh-token",
                        Duration.ofDays(7)));

        // 로그인 요청 및 응답 확인
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "loginId": "Test@example.com",
                          "password": "Password123!"
                        }
                        """))
                .andExpect(status().isOk())

                .andExpect(cookie().value("moamu_refresh", "test-refresh-token"))
                .andExpect(cookie().httpOnly("moamu_refresh", true))
                .andExpect(cookie().secure("moamu_refresh", true))
                .andExpect(cookie().path("moamu_refresh", "/api/users"))
                .andExpect(cookie().maxAge("moamu_refresh", 604800))

                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken")
                        .value("test-access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        // 검증된 회원 ID로 토큰을 발급했는지 확인
        verify(jwtTokenProvider).createAccessToken(10L);
    }

    // 로그인 정보가 일치하지 않으면 401 응답을 반환
    @Test
    void invalidCredentialsReturn401() throws Exception {

        // 서비스에서 로그인 검증에 실패한 상황 설정
        when(userService.login(any(UserLoginRequest.class)))
                .thenThrow(
                        new BusinessException(UserErrorCode.INVALID_CREDENTIALS));

        // 형식은 올바르지만 인증에 실패하는 로그인 요청
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "loginId": "test@example.com",
                          "password": "WrongPassword123!"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code")
                        .value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 로그인에 실패하면 토큰 발급기를 호출하지 않는지 확인
        verifyNoInteractions(jwtTokenProvider, refreshTokenService);
    }
}