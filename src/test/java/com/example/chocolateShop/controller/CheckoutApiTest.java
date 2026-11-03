package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.auth.AuthResponse;
import com.example.chocolateShop.dtos.address.AddressRequest;
import com.example.chocolateShop.dtos.address.AddressResponse;
import com.example.chocolateShop.dtos.cart.AddCartItemRequest;
import com.example.chocolateShop.dtos.category.CategoryRequest;
import com.example.chocolateShop.dtos.category.CategoryResponse;
import com.example.chocolateShop.dtos.checkout.CheckoutRequest;
import com.example.chocolateShop.dtos.checkout.OrderResponse;
import com.example.chocolateShop.dtos.coupon.CouponRequest;
import com.example.chocolateShop.dtos.product.ProductRequest;
import com.example.chocolateShop.dtos.product.ProductResponse;
import com.example.chocolateShop.entity.PaymentMethod;
import com.example.chocolateShop.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CheckoutApiTest extends BaseIntegrationTest {

    private static final String CHECKOUT_URL = "/api/v1/checkout";
    private static final String CART_URL = "/api/v1/cart";
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String ADDRESSES_URL = "/api/v1/addresses";

    @Test
    @DisplayName("POST /checkout - empty cart returns 400")
    void checkout_withEmptyCart_returnsBadRequest() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        Long addressId = createAddress(auth.accessToken());

        CheckoutRequest request = new CheckoutRequest(addressId, null, null, PaymentMethod.COD);

        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /checkout - unknown address id returns 404")
    void checkout_withUnknownAddress_returnsNotFound() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Some Bar", new BigDecimal("5.00"), 10);
        addItem(auth.accessToken(), product.id(), 1);

        CheckoutRequest request = new CheckoutRequest(999L, null, null, PaymentMethod.COD);

        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /checkout - COD order: totals correct, stock decremented, cart cleared")
    void checkout_withCod_succeedsAndSideEffectsApply() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "COD Bar", new BigDecimal("10.00"), 10);
        addItem(auth.accessToken(), product.id(), 2); // subtotal = 20.00, under free-shipping threshold
        Long addressId = createAddress(auth.accessToken());

        CheckoutRequest request = new CheckoutRequest(addressId, null, null, PaymentMethod.COD);

        MvcResult result = mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(20.00))
                .andExpect(jsonPath("$.discountTotal").value(0))
                .andExpect(jsonPath("$.shippingFee").value(5.00))
                .andExpect(jsonPath("$.grandTotal").value(25.00))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.payment.method").value("COD"))
                .andExpect(jsonPath("$.payment.status").value("PENDING"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn();

        OrderResponse order = objectMapper.readValue(result.getResponse().getContentAsString(), OrderResponse.class);

        // cart is now empty
        mockMvc.perform(get(CART_URL).header("Authorization", bearer(auth.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        // stock decremented from 10 to 8
        mockMvc.perform(get("/api/v1/products/" + product.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(8));

        // order shows up in order history
        mockMvc.perform(get(ORDERS_URL).header("Authorization", bearer(auth.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(order.id()));
    }

    @Test
    @DisplayName("POST /checkout - subtotal at/above free-shipping threshold waives the shipping fee")
    void checkout_atFreeShippingThreshold_waivesShippingFee() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Bulk Bar", new BigDecimal("10.00"), 20);
        addItem(auth.accessToken(), product.id(), 5); // subtotal = 50.00, exactly at threshold
        Long addressId = createAddress(auth.accessToken());

        CheckoutRequest request = new CheckoutRequest(addressId, null, null, PaymentMethod.COD);

        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shippingFee").value(0))
                .andExpect(jsonPath("$.grandTotal").value(50.00));
    }

    @Test
    @DisplayName("POST /checkout - insufficient stock returns 409 and does not create an order")
    void checkout_insufficientStock_returnsConflictAndNoOrderCreated() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Tight Stock Bar", new BigDecimal("5.00"), 2);
        addItem(auth.accessToken(), product.id(), 2); // valid when added

        // admin sells out the stock via a direct update before checkout completes
        reduceStockToZero(adminToken, product);

        Long addressId = createAddress(auth.accessToken());
        CheckoutRequest request = new CheckoutRequest(addressId, null, null, PaymentMethod.COD);

        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        mockMvc.perform(get(ORDERS_URL).header("Authorization", bearer(auth.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("POST /checkout - unknown coupon code returns 400")
    void checkout_withUnknownCoupon_returnsBadRequest() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Coupon Bar", new BigDecimal("10.00"), 10);
        addItem(auth.accessToken(), product.id(), 1);
        Long addressId = createAddress(auth.accessToken());

        CheckoutRequest request = new CheckoutRequest(addressId, null, "DOESNOTEXIST", PaymentMethod.COD);

        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /checkout - valid percent coupon reduces the grand total")
    void checkout_withValidCoupon_appliesDiscount() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Discounted Checkout Bar", new BigDecimal("20.00"), 10);
        addItem(auth.accessToken(), product.id(), 1); // subtotal = 20.00
        Long addressId = createAddress(auth.accessToken());

        createCoupon(adminToken, "SAVE10", new BigDecimal("10"), null);

        CheckoutRequest request = new CheckoutRequest(addressId, null, "SAVE10", PaymentMethod.COD);

        mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(20.00))
                .andExpect(jsonPath("$.discountTotal").value(2.00)) // 10% of 20
                .andExpect(jsonPath("$.couponCode").value("SAVE10"));
    }

    @Test
    @DisplayName("POST /orders/{id}/payment/confirm - simulated confirmation endpoint is disabled")
    void simulatedPaymentConfirmation_isNotFound() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Card Bar", new BigDecimal("10.00"), 10);
        addItem(auth.accessToken(), product.id(), 1);
        Long addressId = createAddress(auth.accessToken());

        CheckoutRequest request = new CheckoutRequest(addressId, null, null, PaymentMethod.CARD);
        MvcResult checkoutResult = mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.status").value("PENDING"))
                .andReturn();

        OrderResponse order = objectMapper.readValue(
                checkoutResult.getResponse().getContentAsString(), OrderResponse.class);

        mockMvc.perform(post(ORDERS_URL + "/" + order.id() + "/payment/confirm")
                        .header("Authorization", bearer(auth.accessToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /orders/{id} - a user cannot view another user's order")
    void getOrder_ownershipEnforced() throws Exception {
        AuthResponse ownerAuth = registerCustomer();
        AuthResponse strangerAuth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Private Bar", new BigDecimal("5.00"), 10);
        addItem(ownerAuth.accessToken(), product.id(), 1);
        Long addressId = createAddress(ownerAuth.accessToken());

        CheckoutRequest request = new CheckoutRequest(addressId, null, null, PaymentMethod.COD);
        MvcResult result = mockMvc.perform(post(CHECKOUT_URL)
                        .header("Authorization", bearer(ownerAuth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        OrderResponse order = objectMapper.readValue(result.getResponse().getContentAsString(), OrderResponse.class);

        mockMvc.perform(get(ORDERS_URL + "/" + order.id())
                        .header("Authorization", bearer(strangerAuth.accessToken())))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private void addItem(String token, Long productId, int quantity) throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(productId, quantity);
        mockMvc.perform(post(CART_URL + "/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private Long createAddress(String token) throws Exception {
        AddressRequest request = new AddressRequest(
                "Home", "Test Customer", "+8801700000000", "123 Home St", null,
                "Dhaka", "Dhaka", "1207", "Bangladesh", null);
        MvcResult result = mockMvc.perform(post(ADDRESSES_URL)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AddressResponse.class).id();
    }

    private ProductResponse createProduct(String adminToken, String name, BigDecimal price, int stock) throws Exception {
        var categoryRequest = new CategoryRequest("Checkout Test " + System.nanoTime(), null);
        MvcResult categoryResult = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(), CategoryResponse.class);

        ProductRequest productRequest = new ProductRequest(
                category.id(), name, "desc", price, null, stock, true);
        MvcResult productResult = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(productResult.getResponse().getContentAsString(), ProductResponse.class);
    }

    private void reduceStockToZero(String adminToken, ProductResponse product) throws Exception {
        ProductRequest updateRequest = new ProductRequest(
                product.categoryId(), product.name(), product.description(),
                product.price(), product.discountPrice(), 0, true);
        mockMvc.perform(put("/api/v1/admin/products/" + product.id())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    private void createCoupon(String adminToken, String code, BigDecimal percent, BigDecimal amount) throws Exception {
        CouponRequest request = new CouponRequest(
                code, percent, amount,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30),
                null, true);
        mockMvc.perform(post("/api/v1/admin/coupons")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
