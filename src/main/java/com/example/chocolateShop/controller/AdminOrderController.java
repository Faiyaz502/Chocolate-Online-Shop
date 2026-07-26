package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.admin.AdminOrderResponse;
import com.example.chocolateShop.dtos.admin.AdminOrderSummaryResponse;
import com.example.chocolateShop.dtos.common.CursorPageResponse;
import com.example.chocolateShop.dtos.order.UpdateOrderStatusRequest;
import com.example.chocolateShop.entity.OrderStatus;
import com.example.chocolateShop.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<CursorPageResponse<AdminOrderSummaryResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(adminOrderService.list(status, cursor, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminOrderService.getById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminOrderResponse> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(adminOrderService.updateStatus(id, request.status()));
    }
}
