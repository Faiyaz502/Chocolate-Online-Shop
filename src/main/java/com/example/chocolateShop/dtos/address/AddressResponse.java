package com.example.chocolateShop.dtos.address;

import lombok.Builder;

@Builder
public record AddressResponse(
        Long id,
        String label,
        String recipientName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean isDefault
) {}
