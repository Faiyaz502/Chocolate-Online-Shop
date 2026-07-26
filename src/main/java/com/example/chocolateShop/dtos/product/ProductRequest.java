package com.example.chocolateShop.dtos.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(

        @NotNull(message = "Category is required")
        Long categoryId,

        @NotBlank(message = "Product name is required")
        @Size(max = 200)
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must not be negative")
        BigDecimal price,

        @DecimalMin(value = "0.0", inclusive = true, message = "Discount price must not be negative")
        BigDecimal discountPrice,

        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity must not be negative")
        Integer stockQuantity,

        Boolean active,

        @Valid
        List<ProductImageRequest> images
) {}
