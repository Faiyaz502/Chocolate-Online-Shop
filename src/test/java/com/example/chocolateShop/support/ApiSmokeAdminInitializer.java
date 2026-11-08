package com.example.chocolateShop.support;

import com.example.chocolateShop.entity.User;
import com.example.chocolateShop.enums.Role;
import com.example.chocolateShop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Test-profile-only administrator used for local curl smoke checks. */
@Component
@Profile("test")
@RequiredArgsConstructor
public class ApiSmokeAdminInitializer implements CommandLineRunner {

    public static final String EMAIL = "api-smoke-admin@example.test";
    public static final String PASSWORD = "ApiSmokeAdmin1";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(EMAIL)) {
            return;
        }

        userRepository.save(User.builder()
                .firstName("API")
                .lastName("Smoke Admin")
                .email(EMAIL)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(Role.ADMIN)
                .enabled(true)
                .build());
    }
}
