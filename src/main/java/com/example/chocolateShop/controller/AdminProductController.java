package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.common.CursorPageResponse;
import com.example.chocolateShop.dtos.product.ProductRequest;
import com.example.chocolateShop.dtos.product.ProductResponse;
import com.example.chocolateShop.dtos.product.ProductSort;
import com.example.chocolateShop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> addImage(@PathVariable Long id,
                                                     @RequestParam("file") MultipartFile file,
                                                     @RequestParam(required = false) Integer displayOrder,
                                                     @RequestParam(required = false) Boolean primary) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addImage(id, file, displayOrder, primary));
    }

    @PutMapping(value = "/{id}/images/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> replaceImage(@PathVariable Long id,
                                                         @PathVariable Long imageId,
                                                         @RequestParam("file") MultipartFile file,
                                                         @RequestParam(required = false) Integer displayOrder,
                                                         @RequestParam(required = false) Boolean primary) {
        return ResponseEntity.ok(productService.replaceImage(id, imageId, file, displayOrder, primary));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ProductResponse> removeImage(@PathVariable Long id, @PathVariable Long imageId) {
        return ResponseEntity.ok(productService.removeImage(id, imageId));
    }
}
