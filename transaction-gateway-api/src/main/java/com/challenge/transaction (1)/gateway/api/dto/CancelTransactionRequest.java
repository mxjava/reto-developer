package com.challenge.transaction.gateway.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CancelTransactionRequest(
        @NotNull @Positive Long id,
        @NotBlank @Pattern(regexp = "\\d{6}") String referencia,
        @NotBlank @Pattern(regexp = "(?i)^cancelar$", message = "estatus debe ser cancelar")
                String estatus) {}
