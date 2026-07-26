package com.example.chocolateShop.dtos.category;

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
