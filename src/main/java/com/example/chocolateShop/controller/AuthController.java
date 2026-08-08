package com.example.chocolateShop.controller;


import com.example.chocolateShop.dtos.auth.AuthResponse;
import com.example.chocolateShop.dtos.auth.LoginRequest;
import com.example.chocolateShop.dtos.auth.RefreshTokenRequest;
import com.example.chocolateShop.dtos.auth.RegisterRequest;
import com.example.chocolateShop.service.AuthService;
import com.example.chocolateShop.service.CartService;
import com.example.chocolateShop.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CartService cartService;


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request);
        claimGuestCart(httpRequest, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request);
        claimGuestCart(httpRequest, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * If this register/login request came in from an IP that had an active
     * guest cart, merge it into the now-identified user's cart so items
     * they added before logging in aren't lost.
     */
    private void claimGuestCart(HttpServletRequest httpRequest, AuthResponse response) {
        String ip = ClientIpUtil.resolve(httpRequest);
        cartService.claimGuestCart(ip, response.userId());
    }
}
