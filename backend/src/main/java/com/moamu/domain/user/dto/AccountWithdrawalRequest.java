package com.moamu.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountWithdrawalRequest(
    @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    String currentPassword
) {
}