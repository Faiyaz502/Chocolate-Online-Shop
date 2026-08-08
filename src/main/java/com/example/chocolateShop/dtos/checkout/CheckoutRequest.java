package com.example.chocolateShop.dtos.checkout;


import com.example.chocolateShop.dtos.address.AddressRequest;
import com.example.chocolateShop.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Exactly one of {@code shippingAddressId} (an existing saved address --
 * only usable when logged in) or {@code shippingAddress} (a one-off inline
 * address) must be provided; validated in CheckoutService since it's a
 * cross-field rule.
 * <p>
 * {@code guestEmail} is required for guest checkout (no account to send
 * order updates to otherwise) and ignored for logged-in users, whose
 * account email is used instead.
 */
public record CheckoutRequest(

        Long shippingAddressId,

        @Valid
        AddressRequest shippingAddress,

        String guestEmail,

        String couponCode,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod
) {}

