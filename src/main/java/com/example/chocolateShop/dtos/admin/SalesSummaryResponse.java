package com.example.chocolateShop.dtos.admin;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;

@Builder
public record SalesSummaryResponse(
        BigDecimal totalRevenue,
        long totalOrders,
        Map<String, Long> ordersByStatus,
        long totalCustomers,
        long totalProducts,
        long totalCategories,
        long lowStockProductCount
) {}
