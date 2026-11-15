package com.moamu.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailVerificationVerifyRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "올바른 이메일 형식으로 입력해 주세요.")
        @Size(max = 50, message = "이메일은 50자 이하여야 합니다.")
        @Pattern(regexp = "^\\S+$", message = "이메일에는 공백을 포함할 수 없습니다.")
        String email,

        @NotBlank(message = "인증번호를 입력해 주세요.")
        @Pattern(regexp = "[0-9]{6}", message = "인증번호는 숫자 6자리여야 합니다.")
        String code
) {
}