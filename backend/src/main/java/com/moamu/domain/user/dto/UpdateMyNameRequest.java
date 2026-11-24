package com.moamu.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyNameRequest(
    @NotBlank
    @Size(max = 100)
    String name
) {
}