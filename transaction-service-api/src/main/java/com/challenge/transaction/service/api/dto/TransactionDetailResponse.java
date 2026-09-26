package com.challenge.transaction.service.api.dto;

import java.math.BigDecimal;

public record TransactionDetailResponse(
        Long id,
        String operacion,
        BigDecimal importe,
        String cliente,
        String referencia,
        String estatus) {}
