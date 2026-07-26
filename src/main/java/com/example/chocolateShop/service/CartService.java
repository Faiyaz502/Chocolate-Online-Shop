package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.cart.AddCartItemRequest;
import com.example.chocolateShop.dtos.cart.CartItemResponse;
import com.example.chocolateShop.dtos.cart.CartResponse;
import com.example.chocolateShop.exceptions.InsufficientStockException;
import com.example.chocolateShop.exceptions.ResourceNotFoundException;
import com.example.chocolateShop.entity.*;
import com.example.chocolateShop.repository.CartItemRepository;
import com.example.chocolateShop.repository.CartRepository;
import com.example.chocolateShop.repository.ProductRepository;
import com.example.chocolateShop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.productId()));

        var existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

        int requestedTotalQty = request.quantity() + existing.map(CartItem::getQuantity).orElse(0);
        ensureStockAvailable(product, requestedTotalQty);

        if (existing.isPresent()) {
            existing.get().setQuantity(requestedTotalQty);
        } else {
            CartItem item = CartItem.builder()
                    .product(product)
                    .quantity(request.quantity())
                    .build();
            cart.addOrIncrementItem(item);
        }

        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product " + productId + " is not in your cart"));

        if (quantity == 0) {
            cart.removeItem(item);
            cartItemRepository.delete(item);
        } else {
            ensureStockAvailable(item.getProduct(), quantity);
            item.setQuantity(quantity);
        }

        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product " + productId + " is not in your cart"));

        cart.removeItem(item);
        cartItemRepository.delete(item);

        return toResponse(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private void ensureStockAvailable(Product product, int requestedQuantity) {
        if (!product.isActive()) {
            throw new InsufficientStockException("'" + product.getName() + "' is no longer available");
        }
        if (product.getStockQuantity() < requestedQuantity) {
            throw new InsufficientStockException(
                    "Only " + product.getStockQuantity() + " unit(s) of '" + product.getName() + "' left in stock");
        }
    }

    Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });
    }

    private CartResponse toResponse(Cart cart) {
        var itemResponses = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemResponses.stream().mapToInt(CartItemResponse::quantity).sum();

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalItems(totalItems)
                .subtotal(subtotal)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        BigDecimal unitPrice = product.getEffectivePrice();
        String primaryImage = product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> product.getImages().stream().findFirst())
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .primaryImageUrl(primaryImage)
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .availableStock(product.getStockQuantity())
                .build();
    }
}
