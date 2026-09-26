package com.challenge.transaction.gateway.api.controller;

import com.challenge.transaction.gateway.api.dto.LoginRequest;
import com.challenge.transaction.gateway.api.dto.LoginResponse;
import com.challenge.transaction.gateway.application.TransactionGatewayService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final TransactionGatewayService service;

    public AuthController(TransactionGatewayService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }
}
