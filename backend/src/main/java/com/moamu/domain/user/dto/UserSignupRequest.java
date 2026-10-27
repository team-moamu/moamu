package com.moamu.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor 
public class UserSignupRequest {

  @NotBlank (message = "아이디를 입력해주세요.")
  @Size (max = 50, message = "아이디는 50자 이하여야 합니다.")
  private String loginId;

  @NotBlank(message = "비밀번호를 입력해주세요.")
  private String passwork;

  @NotBlank(message = "이름을 입력해주세요.")
  @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
  private String name;
  
}