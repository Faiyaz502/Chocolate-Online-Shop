package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.checkout.OrderResponse;
import com.example.chocolateShop.exceptions.ResourceNotFoundException;
import com.example.chocolateShop.entity.*;
import com.example.chocolateShop.repository.OrderRepository;
import com.example.chocolateShop.util.OwnerRef;
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

    public List<OrderResponse> getMyOrders(OwnerRef owner) {
        List<Order> orders = owner.isGuest()
                ? orderRepository.findByIpAddressOrderByCreatedAtDesc(owner.ipAddress())
                : orderRepository.findByUserIdOrderByCreatedAtDesc(owner.userId());
        return orders.stream().map(OrderMapper::toResponse).toList();
    }

    public OrderResponse getMyOrder(OwnerRef owner, Long orderId) {
        Order order = findOwnedOrder(owner, orderId);
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
    public OrderResponse confirmCardPayment(OwnerRef owner, Long orderId) {
        Order order = findOwnedOrder(owner, orderId);

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

    private Order findOwnedOrder(OwnerRef owner, Long orderId) {
        return owner.isGuest()
                ? orderRepository.findByIdAndIpAddress(orderId, owner.ipAddress())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId))
                : orderRepository.findByIdAndUserId(orderId, owner.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }
}
