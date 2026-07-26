package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.admin.AdminOrderResponse;
import com.example.chocolateShop.dtos.admin.AdminOrderSummaryResponse;
import com.example.chocolateShop.dtos.checkout.OrderItemResponse;
import com.example.chocolateShop.dtos.checkout.OrderResponse;
import com.example.chocolateShop.dtos.checkout.PaymentResponse;
import com.example.chocolateShop.entity.Order;
import com.example.chocolateShop.entity.OrderItem;
import com.example.chocolateShop.entity.Payment;

final class OrderMapper {

    private OrderMapper() {}

    static OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .discountTotal(order.getDiscountTotal())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getGrandTotal())
                .couponCode(order.getCoupon() != null ? order.getCoupon().getCode() : null)
                .shippingAddressId(order.getShippingAddress().getId())
                .items(order.getItems().stream().map(OrderMapper::toItemResponse).toList())
                .payment(order.getPayment() != null ? toPaymentResponse(order.getPayment()) : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    static AdminOrderResponse toAdminResponse(Order order) {
        return AdminOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .customerId(order.getUser().getId())
                .customerName(order.getUser().getFirstName() + " " + order.getUser().getLastName())
                .customerEmail(order.getUser().getEmail())
                .subtotal(order.getSubtotal())
                .discountTotal(order.getDiscountTotal())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getGrandTotal())
                .couponCode(order.getCoupon() != null ? order.getCoupon().getCode() : null)
                .shippingAddressId(order.getShippingAddress().getId())
                .items(order.getItems().stream().map(OrderMapper::toItemResponse).toList())
                .payment(order.getPayment() != null ? toPaymentResponse(order.getPayment()) : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    static AdminOrderSummaryResponse toAdminSummary(Order order) {
        return AdminOrderSummaryResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .customerName(order.getUser().getFirstName() + " " + order.getUser().getLastName())
                .customerEmail(order.getUser().getEmail())
                .grandTotal(order.getGrandTotal())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .lineTotal(item.getLineTotal())
                .build();
    }

    private static PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .providerReference(payment.getProviderReference())
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .build();
    }
}

