package com.example.chocolateShop.support;

import com.example.chocolateShop.dtos.auth.AuthResponse;
import com.example.chocolateShop.dtos.auth.LoginRequest;
import com.example.chocolateShop.dtos.auth.RegisterRequest;
import com.example.chocolateShop.entity.User;
import com.example.chocolateShop.enums.Role;
import com.example.chocolateShop.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared plumbing for all API integration tests: real Spring context,
 * H2 in-memory DB with the actual Flyway schema, MockMvc, and helpers for
 * getting authenticated customer/admin tokens.
 * <p>
 * Each test method runs in its own transaction that's rolled back afterward,
 * so every test starts from a clean, empty database -- no test ordering
 * dependencies, no leftover data between tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected String uniqueEmail(String prefix) {
        return prefix + "_" + System.nanoTime() + "@example.com";
    }

    protected AuthResponse registerCustomer(String email, String password) throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "Customer", email, password, null);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    protected AuthResponse registerCustomer() throws Exception {
        return registerCustomer(uniqueEmail("customer"), "CustomerPass1");
    }

    protected String loginAndGetAccessToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return auth.accessToken();
    }

    /**
     * Seeds an ADMIN user directly via the repository (there's no public
     * registration endpoint for admins by design) and logs in through the
     * real /auth/login endpoint to get a genuine JWT with the ADMIN role
     * claim baked in.
     */
    protected String createAdminAndGetToken() throws Exception {
        String email = uniqueEmail("admin");
        String rawPassword = "AdminPass1";

        User admin = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);

        return loginAndGetAccessToken(email, rawPassword);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
