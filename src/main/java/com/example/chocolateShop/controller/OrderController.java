package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.checkout.OrderResponse;
import com.example.chocolateShop.security.UserPrincipal;
import com.example.chocolateShop.service.OrderService;
import com.example.chocolateShop.util.OwnerRef;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getMyOrders(resolveOwner(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getMyOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getMyOrder(resolveOwner(principal), id));
    }

    private OwnerRef resolveOwner(UserPrincipal principal) {
        return OwnerRef.ofUser(principal.getId());
    }
}
