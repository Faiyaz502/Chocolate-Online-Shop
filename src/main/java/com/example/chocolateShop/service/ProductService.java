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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
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
    private final ProductImageStorageService imageStorage;

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

        productRepository.saveAndFlush(product);
        return toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        List<String> imageUrls = product.getImages().stream().map(ProductImage::getImageUrl).toList();
        productRepository.delete(product);
        deleteFilesAfterCommit(imageUrls);
    }

    // ---------------------------------------------------------
    // Admin: image management
    // ---------------------------------------------------------

    @Transactional
    public ProductResponse addImage(Long productId, MultipartFile file, Integer displayOrder, Boolean primary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductImageStorageService.StoredProductImage stored = imageStorage.store(file);
        deleteNewFileOnRollback(stored.url());
        boolean makePrimary = Boolean.TRUE.equals(primary) || product.getImages().isEmpty();
        if (makePrimary) {
            product.getImages().forEach(existing -> existing.setPrimary(false));
        }
        ProductImage image = ProductImage.builder()
                .imageUrl(stored.url())
                .displayOrder(displayOrder != null ? displayOrder : product.getImages().size())
                .primary(makePrimary)
                .build();
        product.addImage(image);
        try {
            productRepository.saveAndFlush(product);
            return toResponse(product);
        } catch (RuntimeException ex) {
            imageStorage.deleteManagedUrl(stored.url());
            throw ex;
        }
    }

    @Transactional
    public ProductResponse replaceImage(
            Long productId,
            Long imageId,
            MultipartFile file,
            Integer displayOrder,
            Boolean primary
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        ProductImage image = product.getImages().stream()
                .filter(item -> item.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Image " + imageId + " not found on product " + productId));

        ProductImageStorageService.StoredProductImage stored = imageStorage.store(file);
        deleteNewFileOnRollback(stored.url());
        String oldUrl = image.getImageUrl();
        if (Boolean.TRUE.equals(primary)) {
            product.getImages().forEach(existing -> existing.setPrimary(false));
        }
        image.setImageUrl(stored.url());
        if (displayOrder != null) image.setDisplayOrder(displayOrder);
        if (primary != null) image.setPrimary(primary);

        try {
            productRepository.saveAndFlush(product);
            deleteFilesAfterCommit(List.of(oldUrl));
            return toResponse(product);
        } catch (RuntimeException ex) {
            imageStorage.deleteManagedUrl(stored.url());
            throw ex;
        }
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
        productRepository.saveAndFlush(product);
        deleteFilesAfterCommit(List.of(toRemove.getImageUrl()));
        return toResponse(product);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private void deleteFilesAfterCommit(List<String> imageUrls) {
        Runnable cleanup = () -> imageUrls.forEach(imageStorage::deleteManagedUrl);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
        } else {
            cleanup.run();
        }
    }

    private void deleteNewFileOnRollback(String imageUrl) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        imageStorage.deleteManagedUrl(imageUrl);
                    }
                }
            });
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
