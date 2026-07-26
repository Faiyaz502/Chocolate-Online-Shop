package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.common.CursorPageResponse;
import com.example.chocolateShop.dtos.product.*;
import com.example.chocolateShop.exceptions.ResourceNotFoundException;
import com.example.chocolateShop.entity.Category;
import com.example.chocolateShop.entity.Product;
import com.example.chocolateShop.entity.ProductImage;
import com.example.chocolateShop.repository.CategoryRepository;
import com.example.chocolateShop.repository.ProductRepository;
import com.example.chocolateShop.util.CursorCodec;
import com.example.chocolateShop.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // ---------------------------------------------------------
    // Listing (cursor pagination)
    // ---------------------------------------------------------

    public CursorPageResponse<ProductResponse> list(
            ProductSort sort,
            Long categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String cursor,
            Integer limit,
            boolean activeOnly
    ) {
        ProductSort effectiveSort = sort != null ? sort : ProductSort.NEWEST;
        int effectiveLimit = clampLimit(limit);
        String trimmedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Long cursorId = null;
        BigDecimal cursorPrice = null;
        if (cursor != null && !cursor.isBlank()) {
            CursorCodec.CursorPayload payload = CursorCodec.decode(cursor);
            cursorId = payload.id();
            cursorPrice = payload.price();
            if (effectiveSort != ProductSort.NEWEST && cursorPrice == null) {
                throw new IllegalArgumentException("Cursor is not valid for the requested sort order");
            }
        }

        // fetch one extra row so we can tell whether there's a next page
        List<Product> fetched = switch (effectiveSort) {
            case NEWEST -> productRepository.findNewestKeyset(
                    activeOnly, categoryId, trimmedKeyword, minPrice, maxPrice,
                    cursorId, effectiveLimit + 1);
            case PRICE_ASC -> productRepository.findPriceAscKeyset(
                    activeOnly, categoryId, trimmedKeyword, minPrice, maxPrice,
                    cursorId, cursorPrice, effectiveLimit + 1);
            case PRICE_DESC -> productRepository.findPriceDescKeyset(
                    activeOnly, categoryId, trimmedKeyword, minPrice, maxPrice,
                    cursorId, cursorPrice, effectiveLimit + 1);
        };

        boolean hasNext = fetched.size() > effectiveLimit;
        List<Product> page = hasNext ? fetched.subList(0, effectiveLimit) : fetched;

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            Product last = page.get(page.size() - 1);
            nextCursor = (effectiveSort == ProductSort.NEWEST)
                    ? CursorCodec.encode(last.getId())
                    : CursorCodec.encode(last.getId(), last.getEffectivePrice());
        }

        return CursorPageResponse.<ProductResponse>builder()
                .items(page.stream().map(this::toResponse).toList())
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    private int clampLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    // ---------------------------------------------------------
    // Single product lookup
    // ---------------------------------------------------------

    public ProductResponse getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
        return toResponse(product);
    }

    // ---------------------------------------------------------
    // Admin: create / update / delete
    // ---------------------------------------------------------

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.categoryId()));

        Product product = Product.builder()
                .category(category)
                .name(request.name())
                .slug(uniqueSlug(request.name()))
                .description(request.description())
                .price(request.price())
                .discountPrice(request.discountPrice())
                .stockQuantity(request.stockQuantity())
                .active(request.active() == null || request.active())
                .build();

        applyImages(product, request.images());

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (!product.getCategory().getId().equals(request.categoryId())) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + request.categoryId()));
            product.setCategory(category);
        }

        if (!product.getName().equals(request.name())) {
            product.setSlug(uniqueSlug(request.name()));
        }

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setDiscountPrice(request.discountPrice());
        product.setStockQuantity(request.stockQuantity());
        if (request.active() != null) {
            product.setActive(request.active());
        }

        if (request.images() != null) {
            product.getImages().clear();
            applyImages(product, request.images());
        }

        return toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    // ---------------------------------------------------------
    // Admin: image management
    // ---------------------------------------------------------

    @Transactional
    public ProductResponse addImage(Long productId, ProductImageRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductImage image = ProductImage.builder()
                .imageUrl(request.imageUrl())
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : product.getImages().size())
                .primary(Boolean.TRUE.equals(request.primary()))
                .build();

        if (image.isPrimary()) {
            product.getImages().forEach(existing -> existing.setPrimary(false));
        }

        product.addImage(image);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse removeImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductImage toRemove = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Image " + imageId + " not found on product " + productId));

        product.removeImage(toRemove);
        return toResponse(product);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private void applyImages(Product product, List<ProductImageRequest> images) {
        if (images == null || images.isEmpty()) return;

        boolean anyPrimarySet = images.stream().anyMatch(i -> Boolean.TRUE.equals(i.primary()));
        int order = 0;
        for (ProductImageRequest imgReq : images) {
            ProductImage image = ProductImage.builder()
                    .imageUrl(imgReq.imageUrl())
                    .displayOrder(imgReq.displayOrder() != null ? imgReq.displayOrder() : order)
                    .primary(anyPrimarySet
                            ? Boolean.TRUE.equals(imgReq.primary())
                            : order == 0) // default: first image is primary if none specified
                    .build();
            product.addImage(image);
            order++;
        }
    }

    private String uniqueSlug(String name) {
        String base = SlugUtil.toSlug(name);
        String candidate = base;
        int suffix = 1;
        while (productRepository.existsBySlug(candidate)) {
            candidate = base + "-" + (++suffix);
        }
        return candidate;
    }

    private ProductResponse toResponse(Product product) {
        List<ProductImageResponse> imageResponses = new ArrayList<>();
        for (ProductImage image : product.getImages()) {
            imageResponses.add(ProductImageResponse.builder()
                    .id(image.getId())
                    .imageUrl(image.getImageUrl())
                    .displayOrder(image.getDisplayOrder())
                    .primary(image.isPrimary())
                    .build());
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .effectivePrice(product.getEffectivePrice())
                .stockQuantity(product.getStockQuantity())
                .active(product.isActive())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .images(imageResponses)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
