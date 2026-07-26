package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.admin.AdminOrderResponse;
import com.example.chocolateShop.dtos.admin.AdminOrderSummaryResponse;
import com.example.chocolateShop.dtos.common.CursorPageResponse;
import com.example.chocolateShop.entity.Order;
import com.example.chocolateShop.entity.OrderStatus;
import com.example.chocolateShop.exceptions.InvalidOrderStatusTransitionException;
import com.example.chocolateShop.exceptions.ResourceNotFoundException;
import com.example.chocolateShop.repository.OrderRepository;
import com.example.chocolateShop.util.CursorCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    /**
     * Valid forward transitions. PENDING and PROCESSING can also be
     * cancelled; DELIVERED and CANCELLED are terminal states with no
     * further transitions allowed.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private final OrderRepository orderRepository;

    public CursorPageResponse<AdminOrderSummaryResponse> list(OrderStatus status, String cursor, Integer limit) {
        int effectiveLimit = clampLimit(limit);
        Long cursorId = (cursor != null && !cursor.isBlank()) ? CursorCodec.decode(cursor).id() : null;

        List<Order> fetched = orderRepository.findAdminOrdersKeyset(
                status, cursorId, PageRequest.of(0, effectiveLimit + 1));

        boolean hasNext = fetched.size() > effectiveLimit;
        List<Order> page = hasNext ? fetched.subList(0, effectiveLimit) : fetched;

        String nextCursor = (hasNext && !page.isEmpty())
                ? CursorCodec.encode(page.get(page.size() - 1).getId())
                : null;

        return CursorPageResponse.<AdminOrderSummaryResponse>builder()
                .items(page.stream().map(OrderMapper::toAdminSummary).toList())
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    public AdminOrderResponse getById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return OrderMapper.toAdminResponse(order);
    }

    @Transactional
    public AdminOrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == newStatus) {
            return OrderMapper.toAdminResponse(order); // no-op, already in this state
        }

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidOrderStatusTransitionException(
                    "Cannot move order from " + currentStatus + " to " + newStatus
                            + ". Allowed next states: " + (allowed.isEmpty() ? "none (terminal state)" : allowed));
        }

        order.setStatus(newStatus);
        return OrderMapper.toAdminResponse(order);
    }

    private int clampLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
