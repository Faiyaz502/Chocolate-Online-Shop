package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.coupon.CouponRequest;
import com.example.chocolateShop.dtos.coupon.CouponResponse;
import com.example.chocolateShop.exceptions.DuplicateResourceException;
import com.example.chocolateShop.exceptions.InvalidCouponException;
import com.example.chocolateShop.exceptions.ResourceNotFoundException;
import com.example.chocolateShop.entity.Coupon;
import com.example.chocolateShop.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;

    public List<CouponResponse> getAll() {
        return couponRepository.findAllByOrderByValidUntilDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        if (couponRepository.existsByCodeIgnoreCase(request.code())) {
            throw new DuplicateResourceException("A coupon with code '" + request.code() + "' already exists");
        }
        validateDiscountShape(request);

        Coupon coupon = Coupon.builder()
                .code(request.code().toUpperCase())
                .discountPercent(request.discountPercent())
                .discountAmount(request.discountAmount())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .maxUses(request.maxUses())
                .active(request.active() == null || request.active())
                .build();

        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
        validateDiscountShape(request);

        if (!coupon.getCode().equalsIgnoreCase(request.code())
                && couponRepository.existsByCodeIgnoreCase(request.code())) {
            throw new DuplicateResourceException("A coupon with code '" + request.code() + "' already exists");
        }

        coupon.setCode(request.code().toUpperCase());
        coupon.setDiscountPercent(request.discountPercent());
        coupon.setDiscountAmount(request.discountAmount());
        coupon.setValidFrom(request.validFrom());
        coupon.setValidUntil(request.validUntil());
        coupon.setMaxUses(request.maxUses());
        if (request.active() != null) {
            coupon.setActive(request.active());
        }

        return toResponse(coupon);
    }

    @Transactional
    public void delete(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon not found with id: " + id);
        }
        couponRepository.deleteById(id);
    }

    /**
     * Validates the coupon code and returns the discount amount to apply to
     * the given subtotal. Does NOT increment times_used -- the caller
     * (checkout flow) does that only once the order is actually placed.
     */
    public DiscountResult validateAndCalculateDiscount(String code, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new InvalidCouponException("Coupon code '" + code + "' does not exist"));

        if (!coupon.isCurrentlyValid()) {
            throw new InvalidCouponException("Coupon code '" + code + "' is expired, inactive, or has reached its usage limit");
        }

        BigDecimal discount;
        if (coupon.getDiscountPercent() != null) {
            discount = subtotal.multiply(coupon.getDiscountPercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (coupon.getDiscountAmount() != null) {
            discount = coupon.getDiscountAmount();
        } else {
            discount = BigDecimal.ZERO;
        }

        // never discount more than the subtotal itself
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return new DiscountResult(coupon, discount);
    }

    @Transactional
    public void incrementUsage(Coupon coupon) {
        coupon.setTimesUsed(coupon.getTimesUsed() + 1);
        couponRepository.save(coupon);
    }

    private void validateDiscountShape(CouponRequest request) {
        boolean hasPercent = request.discountPercent() != null;
        boolean hasAmount = request.discountAmount() != null;
        if (hasPercent == hasAmount) {
            throw new IllegalArgumentException(
                    "Exactly one of discountPercent or discountAmount must be provided");
        }
        if (!request.validUntil().isAfter(request.validFrom())) {
            throw new IllegalArgumentException("validUntil must be after validFrom");
        }
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountPercent(coupon.getDiscountPercent())
                .discountAmount(coupon.getDiscountAmount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .maxUses(coupon.getMaxUses())
                .timesUsed(coupon.getTimesUsed())
                .active(coupon.isActive())
                .build();
    }

    public record DiscountResult(Coupon coupon, BigDecimal discountAmount) {}
}
