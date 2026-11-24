package com.moamu.domain.challenge.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ArtworkTaskRetryRequest(@NotNull @Positive Integer expectedTaskVersion) {
}
