package com.challenge.transaction.gateway.api.dto;

public record LoginResponse(
        String accessToken, String tokenType, long expiresIn, String username) {}
