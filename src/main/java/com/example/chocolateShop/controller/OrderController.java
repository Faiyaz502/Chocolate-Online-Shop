package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.checkout.OrderResponse;
import com.example.chocolateShop.security.UserPrincipal;
import com.example.chocolateShop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getMyOrders(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getMyOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getMyOrder(principal.getId(), id));
    }

    /**
     * Simulated payment gateway confirmation for CARD orders (see
     * CheckoutService javadoc for why this is a stub, not real Stripe).
     */
    @PostMapping("/{id}/payment/confirm")
    public ResponseEntity<OrderResponse> confirmPayment(@AuthenticationPrincipal UserPrincipal principal,
                                                          @PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirmCardPayment(principal.getId(), id));
    }
}
