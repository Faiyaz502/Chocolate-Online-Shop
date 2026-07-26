package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.admin.SalesSummaryResponse;
import com.example.chocolateShop.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    public ResponseEntity<SalesSummaryResponse> getSalesSummary() {
        return ResponseEntity.ok(adminDashboardService.getSalesSummary());
    }
}
