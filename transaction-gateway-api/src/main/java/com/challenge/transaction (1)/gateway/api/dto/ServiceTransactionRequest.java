package com.challenge.transaction.gateway.api.dto;

import java.math.BigDecimal;

public record ServiceTransactionRequest(
        String operacion, BigDecimal importe, String cliente, String secreto) {}
