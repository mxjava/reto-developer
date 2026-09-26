package com.challenge.transaction.service.api.controller;

import com.challenge.transaction.service.api.dto.CancelTransactionRequest;
import com.challenge.transaction.service.api.dto.CreateTransactionRequest;
import com.challenge.transaction.service.api.dto.PageResponse;
import com.challenge.transaction.service.api.dto.TransactionDetailResponse;
import com.challenge.transaction.service.api.dto.TransactionResponse;
import com.challenge.transaction.service.application.TransactionApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/internal/transactions")
public class TransactionController {
    private final TransactionApplicationService service;

    public TransactionController(TransactionApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_transactions.write')")
    public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
        return service.create(request);
    }

    @PatchMapping("/status")
    @PreAuthorize("hasAuthority('SCOPE_transactions.cancel')")
    public TransactionResponse cancel(@Valid @RequestBody CancelTransactionRequest request) {
        return service.cancel(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_transactions.read')")
    public PageResponse<TransactionDetailResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id,desc")
                    @Pattern(
                            regexp =
                                    "^(id|operacion|importe|cliente|referencia|estatus),(?i:asc|desc)$",
                            message = "sort no permitido")
                    String sort) {
        return service.findAll(page, size, sort);
    }
}
