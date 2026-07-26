package com.example.chocolateShop.controller;


import com.example.chocolateShop.dtos.address.AddressRequest;
import com.example.chocolateShop.dtos.address.AddressResponse;
import com.example.chocolateShop.security.UserPrincipal;
import com.example.chocolateShop.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(addressService.getAllForUser(principal.getId()));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.create(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id) {
        addressService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
