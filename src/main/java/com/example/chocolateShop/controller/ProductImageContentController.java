package com.example.chocolateShop.controller;

import com.example.chocolateShop.service.ProductImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class ProductImageContentController {

    private final ProductImageStorageService imageStorage;

    @GetMapping("/uploads/products/{filename:.+}")
    public ResponseEntity<Resource> getProductImage(@PathVariable String filename) {
        Resource image = imageStorage.load(filename);
        return ResponseEntity.ok()
                .contentType(imageStorage.mediaType(filename))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(image);
    }
}
