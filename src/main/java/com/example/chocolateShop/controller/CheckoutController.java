package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.checkout.CheckoutRequest;
import com.example.chocolateShop.dtos.checkout.OrderResponse;
import com.example.chocolateShop.security.UserPrincipal;
import com.example.chocolateShop.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody CheckoutRequest request) {
        OrderResponse response = checkoutService.checkout(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
