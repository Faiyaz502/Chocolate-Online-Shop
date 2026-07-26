package com.example.chocolateShop.dtos.coupon;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponRequest(

        @NotBlank(message = "Coupon code is required")
        @Size(max = 50)
        String code,

        @DecimalMin(value = "0.01", message = "Discount percent must be greater than 0")
        @DecimalMax(value = "100.0", message = "Discount percent must not exceed 100")
        BigDecimal discountPercent,

        @DecimalMin(value = "0.01", message = "Discount amount must be greater than 0")
        BigDecimal discountAmount,

        @NotNull(message = "Valid-from date is required")
        LocalDateTime validFrom,

        @NotNull(message = "Valid-until date is required")
        LocalDateTime validUntil,

        @Min(value = 1, message = "Max uses must be at least 1 if provided")
        Integer maxUses,

        Boolean active
) {}
