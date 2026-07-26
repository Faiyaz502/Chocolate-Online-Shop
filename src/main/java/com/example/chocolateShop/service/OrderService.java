package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.checkout.OrderResponse;
import com.example.chocolateShop.exceptions.ResourceNotFoundException;
import com.example.chocolateShop.entity.*;
import com.example.chocolateShop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    public OrderResponse getMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return OrderMapper.toResponse(order);
    }

    /**
     * Stand-in for a real payment gateway confirmation (e.g. a Stripe
     * webhook or client-side redirect callback). Marks a pending CARD
     * payment as PAID and advances the order to PROCESSING.
     * <p>
     * Wiring in real Stripe later means replacing the body of this method
     * with a call to the Stripe API / webhook signature verification --
     * the order/payment status model and every caller stay the same.
     */
    @Transactional
    public OrderResponse confirmCardPayment(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Payment payment = order.getPayment();
        if (payment == null || payment.getMethod() != PaymentMethod.CARD) {
            throw new IllegalArgumentException("Order " + orderId + " does not have a pending card payment");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Payment for order " + orderId + " is already " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setProviderReference("SIMULATED-" + UUID.randomUUID());

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.PROCESSING);
        }

        return OrderMapper.toResponse(order);
    }
}
