package com.example.chocolateShop.dtos.product;

import lombok.Builder;

@Builder
public record ProductImageResponse(
        Long id,
        String imageUrl,
        Integer displayOrder,
        boolean primary
) {}
