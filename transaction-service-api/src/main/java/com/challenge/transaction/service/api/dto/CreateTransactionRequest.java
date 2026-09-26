package com.challenge.transaction.service.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateTransactionRequest(
        @NotBlank
                @Size(max = 30)
                @Pattern(regexp = "^[\\p{L} ]+$", message = "operacion solo admite caracteres")
                String operacion,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal importe,
        @NotBlank
                @Size(min = 2, max = 100)
                @Pattern(
                        regexp = "^[\\p{L} .'-]+$",
                        message = "cliente contiene caracteres no permitidos")
                String cliente,
        @NotBlank @Size(max = 500) String secreto) {}
