package com.example.chocolateShop.dtos.address;

import lombok.Builder;

@Builder
public record AddressResponse(
        Long id,
        String recipientName,
        String phone,
        String city,
        String postalCode,
        boolean isDefault,
        String district, String thana) {}
