package com.example.chocolateShop.dtos.auth;

import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String role
) {
    public static AuthResponseBuilder defaultBuilder() {
        return AuthResponse.builder().tokenType("Bearer");
    }
}
