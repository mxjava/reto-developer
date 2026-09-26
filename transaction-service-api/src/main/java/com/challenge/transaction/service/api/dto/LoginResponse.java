package com.challenge.transaction.service.api.dto;

public record LoginResponse(
        String accessToken, String tokenType, long expiresIn, String username) {}
