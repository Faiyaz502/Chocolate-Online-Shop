package com.example.chocolateShop.dtos.address;

import com.example.chocolateShop.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(


        @NotBlank(message = "Recipient name is required")
        @Size(max = 150)
        String recipientName,

        User user ,

        @NotBlank(message = "Phone is required")
        @Size(max = 30)
        String phone,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255)
        String district,

        @Size(max = 255)
        String thana,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20)
        String postalCode,


        Boolean isDefault
) {}
