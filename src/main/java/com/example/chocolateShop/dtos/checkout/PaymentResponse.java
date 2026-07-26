package com.example.chocolateShop.dtos.checkout;


import com.example.chocolateShop.entity.PaymentMethod;
import com.example.chocolateShop.entity.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentResponse(
        Long id,
        PaymentMethod method,
        PaymentStatus status,
        String providerReference,
        BigDecimal amount,
        LocalDateTime paidAt
) {}
