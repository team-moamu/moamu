package com.moamu.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 로그인 요청 정보를 받는 DTO
@Getter
@NoArgsConstructor
public class UserLoginRequest {

    //이메일 입력값
    @NotBlank
    @Email
    private String loginId;

    //비밀번호 입력값
    @NotBlank
    private String password;
}