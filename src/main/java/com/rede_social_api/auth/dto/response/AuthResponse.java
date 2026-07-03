package com.rede_social_api.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
