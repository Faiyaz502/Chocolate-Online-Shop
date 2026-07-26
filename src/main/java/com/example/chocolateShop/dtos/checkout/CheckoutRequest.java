package com.example.chocolateShop.dtos.checkout;


import com.example.chocolateShop.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(

        @NotNull(message = "Shipping address is required")
        Long shippingAddressId,

        String couponCode,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod
) {}
