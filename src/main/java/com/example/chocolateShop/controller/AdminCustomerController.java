package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.admin.AdminCustomerResponse;
import com.example.chocolateShop.dtos.admin.UpdateCustomerStatusRequest;
import com.example.chocolateShop.dtos.common.CursorPageResponse;
import com.example.chocolateShop.service.AdminCustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    @GetMapping
    public ResponseEntity<CursorPageResponse<AdminCustomerResponse>> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(adminCustomerService.list(cursor, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminCustomerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminCustomerService.getById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminCustomerResponse> updateStatus(@PathVariable Long id,
                                                               @Valid @RequestBody UpdateCustomerStatusRequest request) {
        return ResponseEntity.ok(adminCustomerService.setEnabled(id, request.enabled()));
    }
}
