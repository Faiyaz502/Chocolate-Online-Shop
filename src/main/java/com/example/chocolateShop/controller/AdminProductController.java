package com.example.chocolateShop.controller;

import com.example.chocolateShop.Dtos.common.CursorPageResponse;
import com.example.chocolateShop.Dtos.product.ProductImageRequest;
import com.example.chocolateShop.Dtos.product.ProductRequest;
import com.example.chocolateShop.Dtos.product.ProductResponse;
import com.example.chocolateShop.Dtos.product.ProductSort;
import com.example.chocolateShop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /** Same listing as the public endpoint, but includes inactive/out-of-catalog products too. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<ProductResponse>> list(
            @RequestParam(required = false, defaultValue = "NEWEST") ProductSort sort,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit
    ) {
        CursorPageResponse<ProductResponse> page = productService.list(
                sort, categoryId, keyword, minPrice, maxPrice, cursor, limit, false);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<ProductResponse> addImage(@PathVariable Long id,
                                                     @Valid @RequestBody ProductImageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addImage(id, request));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ProductResponse> removeImage(@PathVariable Long id, @PathVariable Long imageId) {
        return ResponseEntity.ok(productService.removeImage(id, imageId));
    }
}
