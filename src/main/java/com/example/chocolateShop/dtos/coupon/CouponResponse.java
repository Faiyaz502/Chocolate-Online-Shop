package com.example.chocolateShop.dtos.coupon;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CouponResponse(
        Long id,
        String code,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Integer maxUses,
        Integer timesUsed,
        boolean active
) {}
