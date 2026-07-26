package com.example.chocolateShop;

import com.example.chocolateShop.dtos.auth.AuthResponse;
import com.example.chocolateShop.dtos.auth.LoginRequest;
import com.example.chocolateShop.dtos.auth.RefreshTokenRequest;
import com.example.chocolateShop.dtos.auth.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests for every endpoint exposed by AuthController,
 * run against an in-memory H2 database with the real Flyway schema applied.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // each test runs in its own transaction, rolled back afterwards
class AuthControllerIntegrationTest {

    private static final String BASE_URL = "/api/v1/auth";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest validRegisterRequest;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest(
                "Ada",
                "Lovelace",
                uniqueEmail(),
                "StrongPass1",
                "+8801700000000"
        );
    }

    private String uniqueEmail() {
        return "user_" + System.nanoTime() + "@example.com";
    }

    // ---------------------------------------------------------
    // POST /register
    // ---------------------------------------------------------

    @Test
    @DisplayName("POST /register - valid payload creates a user and returns tokens")
    void register_withValidPayload_returnsCreatedWithTokens() throws Exception {
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value(validRegisterRequest.email()))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /register - duplicate email returns 409 Conflict")
    void register_withDuplicateEmail_returnsConflict() throws Exception {
        // first registration succeeds
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated());

        // second registration with the same email fails
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "An account with email '" + validRegisterRequest.email() + "' already exists"));
    }

    @Test
    @DisplayName("POST /register - weak password fails validation with 400")
    void register_withWeakPassword_returnsBadRequest() throws Exception {
        RegisterRequest weakPasswordRequest = new RegisterRequest(
                "Ada", "Lovelace", uniqueEmail(), "weak", null);

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakPasswordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.password", notNullValue()));
    }

    @Test
    @DisplayName("POST /register - invalid email format fails validation with 400")
    void register_withInvalidEmail_returnsBadRequest() throws Exception {
        RegisterRequest invalidEmailRequest = new RegisterRequest(
                "Ada", "Lovelace", "not-an-email", "StrongPass1", null);

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()));
    }

    @Test
    @DisplayName("POST /register - missing required fields fails validation with 400")
    void register_withBlankFields_returnsBadRequest() throws Exception {
        RegisterRequest blankRequest = new RegisterRequest("", "", "", "", null);

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.lastName", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.password", notNullValue()));
    }

    // ---------------------------------------------------------
    // POST /login
    // ---------------------------------------------------------

    @Test
    @DisplayName("POST /login - correct credentials returns 200 with tokens")
    void login_withCorrectCredentials_returnsOkWithTokens() throws Exception {
        registerUser(validRegisterRequest);

        LoginRequest loginRequest = new LoginRequest(validRegisterRequest.email(), validRegisterRequest.password());

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.email").value(validRegisterRequest.email()));
    }

    @Test
    @DisplayName("POST /login - wrong password returns 401 Unauthorized")
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        registerUser(validRegisterRequest);

        LoginRequest loginRequest = new LoginRequest(validRegisterRequest.email(), "WrongPassword1");

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /login - unknown email returns 401 Unauthorized")
    void login_withUnknownEmail_returnsUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest(uniqueEmail(), "SomePassword1");

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------
    // POST /refresh
    // ---------------------------------------------------------

    @Test
    @DisplayName("POST /refresh - valid refresh token returns a new token pair")
    void refresh_withValidToken_returnsNewTokenPair() throws Exception {
        AuthResponse original = registerUser(validRegisterRequest);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(original.refreshToken());

        MvcResult result = mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andReturn();

        AuthResponse refreshed = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);

        // The refresh token is a fresh UUID every rotation, so this is the
        // reliable signal that rotation happened. The access token can
        // legitimately be identical to the previous one if both were minted
        // within the same second, since JWT claims (including iat) only have
        // second-level precision -- that's expected, not a bug.
        assertThat(refreshed.refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(refreshed.accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("POST /refresh - reusing a rotated (already-used) token is rejected")
    void refresh_withAlreadyRotatedToken_returnsUnauthorized() throws Exception {
        AuthResponse original = registerUser(validRegisterRequest);
        RefreshTokenRequest firstRefresh = new RefreshTokenRequest(original.refreshToken());

        // first use rotates the token successfully
        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRefresh)))
                .andExpect(status().isOk());

        // reusing the same (now-revoked) token must fail
        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRefresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /refresh - unknown token returns 401 Unauthorized")
    void refresh_withUnknownToken_returnsUnauthorized() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("not-a-real-token");

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token not recognized"));
    }

    @Test
    @DisplayName("POST /refresh - blank token fails validation with 400")
    void refresh_withBlankToken_returnsBadRequest() throws Exception {
        RefreshTokenRequest blankRequest = new RefreshTokenRequest("");

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankRequest)))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------
    // POST /logout
    // ---------------------------------------------------------

    @Test
    @DisplayName("POST /logout - revokes the refresh token and returns 204")
    void logout_withValidToken_revokesTokenAndReturnsNoContent() throws Exception {
        AuthResponse original = registerUser(validRegisterRequest);
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(original.refreshToken());

        mockMvc.perform(post(BASE_URL + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        // the same token can no longer be used to refresh
        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /logout - unknown token is idempotent and still returns 204")
    void logout_withUnknownToken_returnsNoContent() throws Exception {
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest("does-not-exist");

        mockMvc.perform(post(BASE_URL + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------
    // Security: protected routes require a valid access token
    // ---------------------------------------------------------

    @Test
    @DisplayName("Protected route without a token returns 401 Unauthorized")
    void protectedRoute_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/some-protected-resource"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Protected route with a valid access token is authenticated (no 401)")
    void protectedRoute_withValidAccessToken_isAuthenticated() throws Exception {
        AuthResponse auth = registerUser(validRegisterRequest);

        // the route itself doesn't exist yet (Phase 2+), so we expect anything
        // other than 401 -- i.e. the JWT filter accepted the token and Spring
        // Security let the request continue to routing, which then 404s.
        mockMvc.perform(get("/api/v1/some-protected-resource")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Protected route with a garbage token returns 401 Unauthorized")
    void protectedRoute_withInvalidToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/some-protected-resource")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Public product listing route is accessible without a token")
    void publicRoute_withoutToken_isNotUnauthorized() throws Exception {
        // Not implemented until Phase 2, but the security chain must permit it
        // through rather than blocking with 401 -- absence of a controller
        // yields 404, not 401.
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private AuthResponse registerUser(RegisterRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }
}