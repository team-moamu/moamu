package com.moamu.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor 
public class UserSignupRequest {

  @NotBlank (message = "이메일을 입력해주세요.")
  @Email(message = "올바른 이메일 형식으로 입력해주세요.")
  @Size (max = 50, message = "아이디는 50자 이하여야 합니다.")
  private String loginId;

  @NotBlank(message = "비밀번호를 입력해주세요.")
  @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
  @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*[0-9])[\\x21-\\x7E]+$",
        message = "비밀번호는 영문과 숫자를 포함하고, 공백 없이 입력해주세요."
)
  private String password;

  @NotBlank(message = "이름을 입력해주세요.")
  @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
  private String name;
  
}