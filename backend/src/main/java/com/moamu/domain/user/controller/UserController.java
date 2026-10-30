package com.moamu.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.dto.UserSignupResponse;
import com.moamu.domain.user.service.UserService;
import com.moamu.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSignupResponse>> signup(
        @Valid
        @RequestBody
        UserSignupRequest request){
            Long userId = userService.signup(request);

            UserSignupResponse response = new UserSignupResponse(userId);

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
                
        
        }
}