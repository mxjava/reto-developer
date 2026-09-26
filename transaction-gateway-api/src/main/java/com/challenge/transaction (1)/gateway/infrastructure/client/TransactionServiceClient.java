package com.challenge.transaction.gateway.infrastructure.client;

import com.challenge.transaction.gateway.api.dto.CancelTransactionRequest;
import com.challenge.transaction.gateway.api.dto.LoginRequest;
import com.challenge.transaction.gateway.api.dto.LoginResponse;
import com.challenge.transaction.gateway.api.dto.PageResponse;
import com.challenge.transaction.gateway.api.dto.ServiceTransactionRequest;
import com.challenge.transaction.gateway.api.dto.TransactionDetailResponse;
import com.challenge.transaction.gateway.api.dto.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "transaction-service",
        url = "${transaction.service.url}",
        configuration = FeignSecurityConfig.class)
public interface TransactionServiceClient {
    @PostMapping("/internal/transactions")
    TransactionResponse create(@RequestBody ServiceTransactionRequest request);

    @PatchMapping("/internal/transactions/status")
    TransactionResponse cancel(@RequestBody CancelTransactionRequest request);

    @GetMapping("/internal/transactions")
    PageResponse<TransactionDetailResponse> findAll(
            @RequestParam int page, @RequestParam int size, @RequestParam String sort);

    @PostMapping("/internal/auth/login")
    LoginResponse login(@RequestBody LoginRequest request);
}
