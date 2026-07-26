package com.example.chocolateShop.dtos.product;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ProductResponse(
        Long id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        BigDecimal discountPrice,
        BigDecimal effectivePrice,
        Integer stockQuantity,
        boolean active,
        Long categoryId,
        String categoryName,
        List<ProductImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
