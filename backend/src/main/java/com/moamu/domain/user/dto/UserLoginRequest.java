package com.moamu.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 로그인 요청 정보 받는 DTO
@Getter
@NoArgsConstructor
public class UserLoginRequest {

    @NotBlank
    @Email
    private String loginId;

    @NotBlank
    private String password;
}