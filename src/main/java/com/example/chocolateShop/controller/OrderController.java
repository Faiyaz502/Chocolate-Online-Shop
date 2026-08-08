package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.checkout.OrderResponse;
import com.example.chocolateShop.security.UserPrincipal;
import com.example.chocolateShop.service.OrderService;
import com.example.chocolateShop.util.ClientIpUtil;
import com.example.chocolateShop.util.OwnerRef;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserPrincipal principal,
                                                           HttpServletRequest request) {
        return ResponseEntity.ok(orderService.getMyOrders(resolveOwner(principal, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getMyOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                    HttpServletRequest request,
                                                    @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getMyOrder(resolveOwner(principal, request), id));
    }

    /**
     * Simulated payment gateway confirmation for CARD orders (see
     * CheckoutService javadoc for why this is a stub, not real Stripe).
     */
    @PostMapping("/{id}/payment/confirm")
    public ResponseEntity<OrderResponse> confirmPayment(@AuthenticationPrincipal UserPrincipal principal,
                                                        HttpServletRequest request,
                                                        @PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirmCardPayment(resolveOwner(principal, request), id));
    }

    private OwnerRef resolveOwner(UserPrincipal principal, HttpServletRequest request) {
        return principal != null
                ? OwnerRef.ofUser(principal.getId())
                : OwnerRef.ofGuest(ClientIpUtil.resolve(request));
    }
}
