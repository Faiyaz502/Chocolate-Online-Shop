package com.example.chocolateShop.dtos.cart;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String productSlug,
        String primaryImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal,
        Integer availableStock
) {}
