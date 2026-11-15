package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.checkout.OrderResponse;
import com.example.chocolateShop.exceptions.ResourceNotFoundException;
import com.example.chocolateShop.entity.Order;
import com.example.chocolateShop.repository.OrderRepository;
import com.example.chocolateShop.util.OwnerRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    public List<OrderResponse> getMyOrders(OwnerRef owner) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(owner.userId());
        return orders.stream().map(OrderMapper::toResponse).toList();
    }

    public OrderResponse getMyOrder(OwnerRef owner, Long orderId) {
        Order order = findOwnedOrder(owner, orderId);
        return OrderMapper.toResponse(order);
    }

    private Order findOwnedOrder(OwnerRef owner, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, owner.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }
}
