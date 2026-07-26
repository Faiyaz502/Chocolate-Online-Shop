package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.cart.AddCartItemRequest;
import com.example.chocolateShop.dtos.cart.CartResponse;
import com.example.chocolateShop.dtos.cart.UpdateCartItemRequest;
import com.example.chocolateShop.security.UserPrincipal;
import com.example.chocolateShop.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                                 @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(principal.getId(), request));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long productId,
                                                    @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(principal.getId(), productId, request.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItem(principal.getId(), productId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        cartService.clearCart(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
