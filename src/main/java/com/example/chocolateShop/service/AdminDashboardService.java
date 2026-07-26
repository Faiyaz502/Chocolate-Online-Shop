package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.admin.SalesSummaryResponse;
import com.example.chocolateShop.entity.OrderStatus;
import com.example.chocolateShop.enums.Role;
import com.example.chocolateShop.repository.CategoryRepository;
import com.example.chocolateShop.repository.OrderRepository;
import com.example.chocolateShop.repository.ProductRepository;
import com.example.chocolateShop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    // products with fewer units than this are flagged as low stock
    private static final int LOW_STOCK_THRESHOLD = 10;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public SalesSummaryResponse getSalesSummary() {
        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status.name(), 0L);
        }
        for (Object[] row : orderRepository.countGroupedByStatus()) {
            OrderStatus status = (OrderStatus) row[0];
            Long count = (Long) row[1];
            ordersByStatus.put(status.name(), count);
        }

        return SalesSummaryResponse.builder()
                .totalRevenue(orderRepository.sumRevenueExcludingCancelled(OrderStatus.CANCELLED))
                .totalOrders(orderRepository.count())
                .ordersByStatus(ordersByStatus)
                .totalCustomers(userRepository.countByRole(Role.CUSTOMER))
                .totalProducts(productRepository.count())
                .totalCategories(categoryRepository.count())
                .lowStockProductCount(productRepository.countByActiveTrueAndStockQuantityLessThan(LOW_STOCK_THRESHOLD))
                .build();
    }
}
