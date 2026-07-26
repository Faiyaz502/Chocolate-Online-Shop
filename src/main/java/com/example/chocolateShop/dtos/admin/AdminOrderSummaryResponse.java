package com.example.chocolateShop.dtos.admin;

import com.example.chocolateShop.entity.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record AdminOrderSummaryResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        String customerName,
        String customerEmail,
        BigDecimal grandTotal,
        LocalDateTime createdAt
) {}
