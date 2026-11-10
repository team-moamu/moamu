package com.moamu.domain.user.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moamu.domain.user.dto.EmailAvailabilityResponse;
import com.moamu.domain.user.dto.UserLoginRequest;
import com.moamu.domain.user.dto.UserLoginResponse;
import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.dto.UserSignupResponse;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.service.UserService;
import com.moamu.global.response.ApiResponse;
import com.moamu.global.security.JwtTokenProvider;
import com.moamu.global.security.RefreshTokenService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 로그인 검증을 통과한 회원의 액세스 토큰 발급
    private final JwtTokenProvider jwtTokenProvider;

    private final RefreshTokenService refreshTokenService;

    @Value("${auth.refresh-cookie-secure:true}")
    private boolean refreshCookieSecure;

    // 신규회원 등록, 생성된 Id 반환
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSignupResponse>> signup(
            @Valid @RequestBody UserSignupRequest request) {
        Long userId = userService.signup(request);

        UserSignupResponse response = new UserSignupResponse(userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));

    }

    // 입력한 이메일의 사용 가능 여부 반환
    @GetMapping("/email-availability")
    public ResponseEntity<ApiResponse<EmailAvailabilityResponse>> checkEmailAvailability(
            @RequestParam("email") @NotBlank @Email @Size(max = 50) @Pattern(regexp = "^\\S+$", message = "이메일에는 공백을 포함할 수 없습니다.") String email) {
        boolean available = userService.isEmailAvailable(email);

        EmailAvailabilityResponse response = new EmailAvailabilityResponse(available);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 이메일과 비밀번호로 로그인하고 액세스 토큰을 반환
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserLoginResponse>> login(
            @Valid @RequestBody UserLoginRequest request) {

        User user = userService.login(request);

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());

        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user.getUserId());

        ResponseCookie cookie = ResponseCookie
                .from("moamu_refresh", refreshToken.token())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/users")
                .maxAge(refreshToken.ttl())
                .build();

        UserLoginResponse response = new UserLoginResponse(accessToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(response));
    }
}