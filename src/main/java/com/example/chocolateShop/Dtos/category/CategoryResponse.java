package com.example.chocolateShop.Dtos.category;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        LocalDateTime createdAt
) {}
