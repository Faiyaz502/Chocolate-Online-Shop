//package com.example.chocolateShop.controller;
//
//
//import com.example.chocolateShop.dtos.cart.AddCartItemRequest;
//import com.example.chocolateShop.dtos.product.ProductResponse;
//import com.example.chocolateShop.support.BaseIntegrationTest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.MediaType;
//
//
//import java.math.BigDecimal;
//
//import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.post;
//import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//
//class GuestCheckoutApiTest extends BaseIntegrationTest {
//
//    private static final String CART_URL = "/api/v1/cart";
//    private static final String CHECKOUT_URL = "/api/v1/checkout";
//    private static final String ORDERS_URL = "/api/v1/orders";
//
//    private static final String GUEST_IP = "203.0.113.10";
//    private static final String OTHER_GUEST_IP = "203.0.113.20";
//
//    @Test
//    @DisplayName("GET /cart - works without a token, guest gets an empty cart")
//    void getCart_asGuest_returnsEmptyCart() throws Exception {
//        mockMvc.perform(get(CART_URL).header("X-Forwarded-For", GUEST_IP))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.items.length()").value(0));
//    }
//
//    @Test
//    @DisplayName("POST /cart/items - guest can add items without a token, cart persists across requests from the same IP")
//    void addItem_asGuest_persistsAcrossRequestsFromSameIp() throws Exception {
//        String adminToken = createAdminAndGetToken();
//        ProductResponse product = createProduct(adminToken, "Guest Bar", new BigDecimal("6.00"), 20);
//
//        mockMvc.perform(post(CART_URL + "/items")
//                        .header("X-Forwarded-For", GUEST_IP)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(new AddCartItemRequest(product.id(), 2))))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.items.length()").value(1));
//
//        // a fresh request from the same IP sees the same cart
//        mockMvc.perform(get(CART_URL).header("X-Forwarded-For", GUEST_IP))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.items.length()").value(1))
//                .andExpect(jsonPath("$.items[0].quantity").value(2));
//    }
//
//    @Test
//    @DisplayName("Two guests on different IPs have fully isolated carts")
//    void carts_areIsolatedByIp() throws Exception {
//        String adminToken = createAdminAndGetToken();
//        ProductResponse product = createProduct(adminToken, "Shared Product", new BigDecimal("4.00"), 20);
//
//        mockMvc.perform(post(CART_URL + "/items")
//                        .header("X-Forwarded-For", GUEST_IP)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(new AddCartItemRequest(product.id(), 1))))
//                .andExpect(status().isOk());
//
//        // second guest, different IP, never added anything -- must see an empty cart
//        mockMvc.perform(get(CART_URL).header("X-Forwarded-For", OTHER_GUEST_IP))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.items.length()").value(0));
//    }
//
//    @Test
//    @DisplayName("POST /checkout - guest checkout without guestEmail is rejected")
//    void checkout_asGuest_withoutEmail_returnsBadRequest() throws Exception {
//        String adminToken = createAdminAndGetToken();
//        ProductResponse product = createProduct(adminToken, "Email Required Bar", new BigDecimal("5.00"), 10);
//        addGuestItem(product.id(), 1);
//
//        CheckoutRequest request = new CheckoutRequest(null, guestAddress(), null, null, PaymentMethod.COD);
//
//        mockMvc.perform(post(CHECKOUT_URL)
//                        .header("X-Forwarded-For", GUEST_IP)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @DisplayName("POST /checkout - guest checkout with a saved shippingAddressId (not inline) is rejected")
//    void checkout_asGuest_withSavedAddressId_returnsBadRequest() throws Exception {
//        String adminToken = createAdminAndGetToken();
//        ProductResponse product = createProduct(adminToken, "No Saved Address Bar", new BigDecimal("5.00"), 10);
//        addGuestItem(product.id(), 1);
//
//        // guests have no account, so a shippingAddressId can never legitimately belong to them
//        CheckoutRequest request = new CheckoutRequest(1L, null, "guest@example.com", null, PaymentMethod.COD);
//
//        mockMvc.perform(post(CHECKOUT_URL)
//                        .header("X-Forwarded-For", GUEST_IP)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @DisplayName("POST /checkout - guest can complete an order with an inline address and email")
//    void checkout_asGuest_succeeds() throws Exception {
//        String adminToken = createAdminAndGetToken();
//        ProductResponse product = createProduct(adminToken, "Guest Checkout Bar", new BigDecimal("10.00"), 10);
//        addGuestItem(product.id(), 2); // subtotal 20.00
//
//        CheckoutRequest request = new CheckoutRequest(
//                null, guestAddress(), "guest@example.com", null, PaymentMethod.COD);
//
//        MvcResult result = mockMvc.perform(post(CHECKOUT_URL)
//                        .header("X-Forwarded-For", GUEST_IP)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.guestOrder").value(true))
//                .andExpect(jsonPath("$.subtotal").value(20.00))
//                .andExpect(jsonPath("$.grandTotal").value(25.00))
//                .andReturn();
//
//        OrderResponse order = objectMapper.readValue(result.getResponse().getContentAsString(), OrderResponse.class);
//
//        // guest's cart was cleared after checkout
//        mockMvc.perform(get(CART_URL).header("X-Forwarded-For", GUEST_IP))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.items.length()").value(0));
//
//        // the order shows up in this guest's order history (same IP)...
//        mockMvc.perform(get(ORDERS_URL).header("X-Forwarded-For", GUEST_IP))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].id").value(order.id()));
//
//        // ...but is invisible to a different guest IP
//        mockMvc.perform(get(ORDERS_URL).header("X-Forwarded-For", OTHER_GUEST_IP))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(0));
//
//        mockMvc.perform(get(ORDERS_URL + "/" + order.id()).header("X-Forwarded-For", OTHER_GUEST_IP))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @DisplayName("Admin view of a guest order shows 'Guest' with no customer account attached")
//    void adminView_ofGuestOrder_showsGuestLabel() throws Exception {
//        String adminToken = createAdminAndGetToken();
//        ProductResponse product = createProduct(adminToken, "Admin Visible Guest Bar", new BigDecimal("8.00"), 10);
//        addGuestItem(product.id(), 1);
//
//        CheckoutRequest request = new CheckoutRequest(
//                null, guestAddress(), "guest@example.com", null, PaymentMethod.COD);
//        MvcResult result = mockMvc.perform(post(CHECKOUT_URL)
//                        .header("X-Forwarded-For", GUEST_IP)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        OrderResponse order = objectMapper.readValue(result.getResponse().getContentAsString(), OrderResponse.class);
//
//        mockMvc.perform(get("/api/v1/admin/orders/" + order.id())
//                        .header("Authorization", bearer(adminToken)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.guestOrder").value(true))
//                .andExpect(jsonPath("$.customerName").value("Guest"))
//                .andExpect(jsonPath("$.customerEmail").value("guest@example.com"))
//                .andExpect(jsonPath("$.customerId").doesNotExist());
//    }
//
//    @Test
//    @DisplayName("Registering an account merges the caller's guest cart into the new account")
//    void register_asFormerGuest_claimsGuestCart() throws Exception {
//        String adminToken = createAdminAndGetToken();
//        ProductResponse product = createProduct(adminToken, "Claimable Bar", new BigDecimal("3.00"), 20);
//        addGuestItem(product.id(), 4);
//
//        RegisterRequest registerRequest = new RegisterRequest(
//                "New", "Convert", uniqueEmail("convert"), "ConvertPass1", null);
//
//        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
//                        .header("X-Forwarded-For", GUEST_IP)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        AuthResponse auth = objectMapper.readValue(registerResult.getResponse().getContentAsString(), AuthResponse.class);
//
//        // the item added as a guest now shows up in the new account's cart
//        mockMvc.perform(get(CART_URL).header("Authorization", bearer(auth.accessToken())))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.items.length()").value(1))
//                .andExpect(jsonPath("$.items[0].quantity").value(4));
//
//        // and the guest cart at that IP is gone
//        mockMvc.perform(get(CART_URL).header("X-Forwarded-For", GUEST_IP))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.items.length()").value(0));
//    }
//
//    // ---------------------------------------------------------
//    // Helpers
//    // ---------------------------------------------------------
//
//    private AddressRequest guestAddress() {
//        return new AddressRequest("Home", "Guest Customer", "+8801700000000",
//                "123 Guest St", null, "Dhaka", "Dhaka", "1207", "Bangladesh", null);
//    }
//
//    private void addGuestItem(Long productId, int quantity) throws Exception {
//        mockMvc.perform(post(CART_URL + "/items")
//                        .header("X-Forwarded-For", GUEST_IP)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(new AddCartItemRequest(productId, quantity))))
//                .andExpect(status().isOk());
//    }
//
//    private ProductResponse createProduct(String adminToken, String name, BigDecimal price, int stock) throws Exception {
//        var categoryRequest = new com.chocolateshop.dto.category.CategoryRequest("Guest Test " + System.nanoTime(), null);
//        MvcResult categoryResult = mockMvc.perform(post("/api/v1/admin/categories")
//                        .header("Authorization", bearer(adminToken))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(categoryRequest)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        CategoryResponse category = objectMapper.readValue(
//                categoryResult.getResponse().getContentAsString(), CategoryResponse.class);
//
//        ProductRequest productRequest = new ProductRequest(
//                category.id(), name, "desc", price, null, stock, true, null);
//        MvcResult productResult = mockMvc.perform(post("/api/v1/admin/products")
//                        .header("Authorization", bearer(adminToken))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(productRequest)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        return objectMapper.readValue(productResult.getResponse().getContentAsString(), ProductResponse.class);
//    }
//}
