package com.moamu.domain.user.controller;

import org.springframework.http.HttpStatus;
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

    //이메일과 비밀번호로 로그인하고 액세스 토큰을 반환  
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserLoginResponse>> login(
            @Valid @RequestBody UserLoginRequest request) {

        // 이메일, 비밀번호 및 회원 상태 검증
        User user = userService.login(request);

        // 검증된 회원의 ID로 액세스 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());

        // 회원 엔티티 대신 응답 전용 DTO로 토큰 전달
        UserLoginResponse response = new UserLoginResponse(accessToken);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}