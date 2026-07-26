package com.example.chocolateShop.dtos.admin;

import jakarta.validation.constraints.NotNull;

public record UpdateCustomerStatusRequest(

        @NotNull(message = "enabled is required")
        Boolean enabled
) {}
