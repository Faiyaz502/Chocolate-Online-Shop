package com.example.chocolateShop.dtos.cart;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal subtotal
) {}
