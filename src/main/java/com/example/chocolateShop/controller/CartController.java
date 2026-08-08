package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.cart.AddCartItemRequest;
import com.example.chocolateShop.dtos.cart.CartResponse;
import com.example.chocolateShop.dtos.cart.UpdateCartItemRequest;
import com.example.chocolateShop.security.UserPrincipal;
import com.example.chocolateShop.service.CartService;
import com.example.chocolateShop.util.ClientIpUtil;
import com.example.chocolateShop.util.OwnerRef;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserPrincipal principal,
                                                HttpServletRequest request) {
        return ResponseEntity.ok(cartService.getCart(resolveOwner(principal, request)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                                HttpServletRequest request,
                                                @Valid @RequestBody AddCartItemRequest addRequest) {
        return ResponseEntity.ok(cartService.addItem(resolveOwner(principal, request), addRequest));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(@AuthenticationPrincipal UserPrincipal principal,
                                                   HttpServletRequest request,
                                                   @PathVariable Long productId,
                                                   @Valid @RequestBody UpdateCartItemRequest updateRequest) {
        OwnerRef owner = resolveOwner(principal, request);
        return ResponseEntity.ok(cartService.updateItemQuantity(owner, productId, updateRequest.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal UserPrincipal principal,
                                                   HttpServletRequest request,
                                                   @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItem(resolveOwner(principal, request), productId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserPrincipal principal,
                                          HttpServletRequest request) {
        cartService.clearCart(resolveOwner(principal, request));
        return ResponseEntity.noContent().build();
    }

    private OwnerRef resolveOwner(UserPrincipal principal, HttpServletRequest request) {
        return principal != null
                ? OwnerRef.ofUser(principal.getId())
                : OwnerRef.ofGuest(ClientIpUtil.resolve(request));
    }
}
