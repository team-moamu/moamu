package com.moamu.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
    @NotBlank(message = "이메일 인증을 완료해 주세요.")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "재설정 토큰 형식이 올바르지 않아요.")
    String resetToken,

    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 해요.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*[0-9])[\\x21-\\x7E]+$",
        message = "비밀번호는 영문과 숫자를 포함하고, 공백 없이 입력해 주세요."
    )
    String newPassword
) {
}