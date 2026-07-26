package com.example.chocolateShop.dtos.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(

        @Size(max = 50)
        String label,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 150)
        String recipientName,

        @NotBlank(message = "Phone is required")
        @Size(max = 30)
        String phone,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255)
        String line1,

        @Size(max = 255)
        String line2,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @Size(max = 100)
        String state,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20)
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 100)
        String country,

        Boolean isDefault
) {}
