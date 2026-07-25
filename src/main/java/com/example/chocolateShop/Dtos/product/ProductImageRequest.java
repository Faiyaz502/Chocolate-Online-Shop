package com.example.chocolateShop.Dtos.product;

import jakarta.validation.constraints.NotBlank;

public record ProductImageRequest(

        @NotBlank(message = "Image URL is required")
        String imageUrl,

        Integer displayOrder,

        Boolean primary
) {}
