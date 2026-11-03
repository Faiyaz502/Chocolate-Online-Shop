package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.cart.AddCartItemRequest;
import com.example.chocolateShop.dtos.cart.CartResponse;
import com.example.chocolateShop.dtos.cart.UpdateCartItemRequest;
import com.example.chocolateShop.security.UserPrincipal;
import com.example.chocolateShop.service.CartService;
import com.example.chocolateShop.util.OwnerRef;
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
        return ResponseEntity.ok(cartService.getCart(resolveOwner(principal)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                                @Valid @RequestBody AddCartItemRequest addRequest) {
        return ResponseEntity.ok(cartService.addItem(resolveOwner(principal), addRequest));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable Long productId,
                                                           @Valid @RequestBody UpdateCartItemRequest updateRequest) {
        return ResponseEntity.ok(cartService.updateItemQuantity(
                resolveOwner(principal), productId, updateRequest.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItem(resolveOwner(principal), productId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        cartService.clearCart(resolveOwner(principal));
        return ResponseEntity.noContent().build();
    }

    private OwnerRef resolveOwner(UserPrincipal principal) {
        return OwnerRef.ofUser(principal.getId());
    }
}
