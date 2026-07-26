package com.example.chocolateShop.dtos.admin;

import com.example.chocolateShop.dtos.checkout.OrderItemResponse;
import com.example.chocolateShop.dtos.checkout.PaymentResponse;
import com.example.chocolateShop.entity.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AdminOrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        Long customerId,
        String customerName,
        String customerEmail,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal shippingFee,
        BigDecimal grandTotal,
        String couponCode,
        Long shippingAddressId,
        List<OrderItemResponse> items,
        PaymentResponse payment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
