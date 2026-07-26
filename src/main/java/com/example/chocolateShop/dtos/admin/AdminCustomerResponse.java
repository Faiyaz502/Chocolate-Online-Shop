package com.example.chocolateShop.dtos.admin;

import com.example.chocolateShop.enums.Role;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminCustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        boolean enabled,
        LocalDateTime createdAt
) {}
