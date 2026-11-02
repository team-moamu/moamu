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
import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.dto.UserSignupResponse;
import com.moamu.domain.user.service.UserService;
import com.moamu.global.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSignupResponse>> signup(
            @Valid 
            @RequestBody UserSignupRequest request) {
        Long userId = userService.signup(request);

        UserSignupResponse response = new UserSignupResponse(userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));

    }

    @GetMapping("/email-availability")
    public ResponseEntity<ApiResponse<EmailAvailabilityResponse>> checkEmailAvailability(
            @RequestParam("email")
            @NotBlank 
            @Email 
            @Size(max = 50) String email) {
        boolean available = userService.isEmailAvailable(email);

        EmailAvailabilityResponse response = new EmailAvailabilityResponse(available);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}