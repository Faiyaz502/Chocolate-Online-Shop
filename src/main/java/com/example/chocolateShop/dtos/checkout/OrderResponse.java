package com.example.chocolateShop.dtos.checkout;


import com.example.chocolateShop.entity.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal shippingFee,
        BigDecimal grandTotal,
        String couponCode,
        Long shippingAddressId,
        List<OrderItemResponse> items,
        PaymentResponse payment,
        boolean guestOrder,
        String guestEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

