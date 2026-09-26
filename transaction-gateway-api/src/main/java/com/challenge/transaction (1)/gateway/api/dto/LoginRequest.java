package com.challenge.transaction.gateway.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
                @Size(min = 3, max = 80)
                @Pattern(
                        regexp = "^[A-Za-z0-9._@-]+$",
                        message = "solo admite letras, numeros y . _ @ -")
                String username,
        @NotBlank @Size(min = 8, max = 128) String password) {}
