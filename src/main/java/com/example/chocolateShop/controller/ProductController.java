package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.common.CursorPageResponse;
import com.example.chocolateShop.dtos.product.ProductResponse;
import com.example.chocolateShop.dtos.product.ProductSort;
import com.example.chocolateShop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Cursor-paginated product listing with search/filter.
     * Example: GET /api/v1/products?categoryId=3&keyword=dark&minPrice=5&maxPrice=50&sort=PRICE_ASC&limit=20&cursor=abc123
     */
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
                sort, categoryId, keyword, minPrice, maxPrice, cursor, limit, true);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getBySlug(slug));
    }
}
