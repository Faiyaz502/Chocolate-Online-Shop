package com.example.chocolateShop.util;

/** Identifies an authenticated user who owns customer data. */
public record OwnerRef(Long userId) {

    public OwnerRef {
        if (userId == null) {
            throw new IllegalArgumentException("An authenticated user ID is required");
        }
    }

    public static OwnerRef ofUser(Long userId) {
        return new OwnerRef(userId);
    }
}
