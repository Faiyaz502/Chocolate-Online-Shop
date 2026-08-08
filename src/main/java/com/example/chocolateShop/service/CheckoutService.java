package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.address.AddressRequest;
import com.example.chocolateShop.dtos.checkout.CheckoutRequest;
import com.example.chocolateShop.dtos.checkout.OrderResponse;

import com.example.chocolateShop.exceptions.EmptyCartException;
import com.example.chocolateShop.exceptions.InsufficientStockException;
import com.example.chocolateShop.exceptions.ResourceNotFoundException;
import com.example.chocolateShop.entity.*;
import com.example.chocolateShop.repository.AddressRepository;
import com.example.chocolateShop.repository.OrderRepository;
import com.example.chocolateShop.repository.ProductRepository;
import com.example.chocolateShop.repository.UserRepository;
import com.example.chocolateShop.util.OwnerRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutService {

    // simple flat-rate shipping: free above this subtotal, flat fee below it
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50.00");
    private static final BigDecimal FLAT_SHIPPING_FEE = new BigDecimal("5.00");

    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CartService cartService;
    private final CouponService couponService;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse checkout(OwnerRef owner, CheckoutRequest request) {
        User user = owner.isGuest()
                ? null
                : userRepository.findById(owner.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + owner.userId()));

        Address address = resolveShippingAddress(owner, user, request);

        Cart cart = cartService.getOrCreateCart(owner);
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Your cart is empty");
        }

        // Re-validate stock at checkout time -- items may have sold out
        // since they were added to the cart.
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.isActive()) {
                throw new InsufficientStockException("'" + product.getName() + "' is no longer available");
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(
                        "Only " + product.getStockQuantity() + " unit(s) of '" + product.getName() + "' left in stock");
            }
        }

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getProduct().getEffectivePrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountTotal = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            CouponService.DiscountResult discountResult =
                    couponService.validateAndCalculateDiscount(request.couponCode(), subtotal);
            appliedCoupon = discountResult.coupon();
            discountTotal = discountResult.discountAmount();
        }

        BigDecimal afterDiscount = subtotal.subtract(discountTotal);
        BigDecimal shippingFee = afterDiscount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : FLAT_SHIPPING_FEE;

        BigDecimal grandTotal = afterDiscount.add(shippingFee).max(BigDecimal.ZERO);

        Order order = Order.builder()
                .user(user)
                .ipAddress(owner.isGuest() ? owner.ipAddress() : null)
                .guestEmail(owner.isGuest() ? request.guestEmail() : null)
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .discountTotal(discountTotal)
                .shippingFee(shippingFee)
                .grandTotal(grandTotal)
                .coupon(appliedCoupon)
                .shippingAddress(address)
                .build();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            BigDecimal unitPrice = product.getEffectivePrice();

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(cartItem.getQuantity())
                    .lineTotal(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();
            order.addItem(orderItem);

            // decrement stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        Payment payment = Payment.builder()
                .method(request.paymentMethod())
                .status(PaymentStatus.PENDING)
                .amount(grandTotal)
                .build();
        order.setPaymentInternal(payment);

        order = orderRepository.save(order);

        if (appliedCoupon != null) {
            couponService.incrementUsage(appliedCoupon);
        }

        cartService.clearCart(owner);

        return OrderMapper.toResponse(order);
    }

    /**
     * Exactly one of a saved address (logged-in users only) or an inline
     * one-off address must be supplied. Guests can only use the inline
     * form, since they have no account to own a saved address.
     */
    private Address resolveShippingAddress(OwnerRef owner, User user, CheckoutRequest request) {
        boolean hasSavedAddressId = request.shippingAddressId() != null;
        boolean hasInlineAddress = request.shippingAddress() != null;

        if (hasSavedAddressId == hasInlineAddress) {
            throw new IllegalArgumentException(
                    "Provide exactly one of shippingAddressId (a saved address) or shippingAddress (a one-off address)");
        }

        if (owner.isGuest()) {
            if (hasSavedAddressId) {
                throw new IllegalArgumentException("Guest checkout requires an inline shippingAddress, not a saved shippingAddressId");
            }
            if (request.guestEmail() == null || request.guestEmail().isBlank()) {
                throw new IllegalArgumentException("guestEmail is required for guest checkout");
            }
            return saveInlineAddress(null, request.shippingAddress());
        }

        if (hasSavedAddressId) {
            return addressRepository.findByIdAndUserId(request.shippingAddressId(), owner.userId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Address not found with id: " + request.shippingAddressId()));
        }

        return saveInlineAddress(user, request.shippingAddress());
    }

    private Address saveInlineAddress(User user, AddressRequest addressRequest) {
        Address address = Address.builder()
                .user(user)
                .thana(addressRequest.thana())
                .recipientName(addressRequest.recipientName())
                .phone(addressRequest.phone())
                .district(addressRequest.district())
                .postalCode(addressRequest.postalCode())
                .isDefault(false)
                .build();
        return addressRepository.save(address);
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(ORDER_NUMBER_DATE_FORMAT);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String candidate = "ORD-" + datePart + "-" + randomPart;
        // astronomically unlikely to collide, but guard anyway
        while (orderRepository.existsByOrderNumber(candidate)) {
            randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            candidate = "ORD-" + datePart + "-" + randomPart;
        }
        return candidate;
    }
}
