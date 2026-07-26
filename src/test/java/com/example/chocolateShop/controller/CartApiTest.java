package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.auth.AuthResponse;
import com.example.chocolateShop.dtos.cart.AddCartItemRequest;
import com.example.chocolateShop.dtos.category.CategoryRequest;
import com.example.chocolateShop.dtos.category.CategoryResponse;
import com.example.chocolateShop.dtos.product.ProductRequest;
import com.example.chocolateShop.dtos.product.ProductResponse;
import com.example.chocolateShop.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CartApiTest extends BaseIntegrationTest {

    private static final String URL = "/api/v1/cart";

    @Test
    @DisplayName("GET /cart - no token returns 401")
    void getCart_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /cart - a new user has an empty cart")
    void getCart_newUser_isEmpty() throws Exception {
        AuthResponse auth = registerCustomer();

        mockMvc.perform(get(URL).header("Authorization", bearer(auth.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.subtotal").value(0));
    }

    @Test
    @DisplayName("POST /cart/items - adds a product and computes the subtotal")
    void addItem_addsProductAndComputesSubtotal() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Cart Bar", new BigDecimal("5.00"), 20);

        AddCartItemRequest request = new AddCartItemRequest(product.id(), 3);

        mockMvc.perform(post(URL + "/items")
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.subtotal").value(15.00));
    }

    @Test
    @DisplayName("POST /cart/items - adding the same product twice increments quantity, not a duplicate row")
    void addItem_sameProductTwice_incrementsQuantity() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Stackable Bar", new BigDecimal("2.00"), 20);

        addItem(auth.accessToken(), product.id(), 2);
        addItem(auth.accessToken(), product.id(), 3);

        mockMvc.perform(get(URL).header("Authorization", bearer(auth.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    @DisplayName("POST /cart/items - requesting more than available stock returns 409")
    void addItem_exceedsStock_returnsConflict() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Scarce Bar", new BigDecimal("5.00"), 2);

        AddCartItemRequest request = new AddCartItemRequest(product.id(), 5);

        mockMvc.perform(post(URL + "/items")
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /cart/items/{productId} - updates quantity")
    void updateItem_changesQuantity() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Adjustable Bar", new BigDecimal("4.00"), 20);
        addItem(auth.accessToken(), product.id(), 2);

        mockMvc.perform(put(URL + "/items/" + product.id())
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(7))
                .andExpect(jsonPath("$.subtotal").value(28.00));
    }

    @Test
    @DisplayName("PUT /cart/items/{productId} with quantity 0 removes the item")
    void updateItem_withZeroQuantity_removesItem() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Removable Bar", new BigDecimal("4.00"), 20);
        addItem(auth.accessToken(), product.id(), 2);

        mockMvc.perform(put(URL + "/items/" + product.id())
                        .header("Authorization", bearer(auth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @DisplayName("DELETE /cart/items/{productId} - removes the item")
    void removeItem_removesFromCart() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse product = createProduct(adminToken, "Kickable Bar", new BigDecimal("4.00"), 20);
        addItem(auth.accessToken(), product.id(), 1);

        mockMvc.perform(delete(URL + "/items/" + product.id())
                        .header("Authorization", bearer(auth.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @DisplayName("DELETE /cart - clears the whole cart")
    void clearCart_removesAllItems() throws Exception {
        AuthResponse auth = registerCustomer();
        String adminToken = createAdminAndGetToken();
        ProductResponse p1 = createProduct(adminToken, "Bar A", new BigDecimal("2.00"), 20);
        ProductResponse p2 = createProduct(adminToken, "Bar B", new BigDecimal("3.00"), 20);
        addItem(auth.accessToken(), p1.id(), 1);
        addItem(auth.accessToken(), p2.id(), 1);

        mockMvc.perform(delete(URL).header("Authorization", bearer(auth.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(URL).header("Authorization", bearer(auth.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private void addItem(String token, Long productId, int quantity) throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(productId, quantity);
        mockMvc.perform(post(URL + "/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private ProductResponse createProduct(String adminToken, String name, BigDecimal price, int stock) throws Exception {
        var categoryRequest = new CategoryRequest("Cart Test " + System.nanoTime(), null);
        MvcResult categoryResult = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        CategoryResponse category = objectMapper.readValue(
                categoryResult.getResponse().getContentAsString(), CategoryResponse.class);

        ProductRequest productRequest = new ProductRequest(
                category.id(), name, "desc", price, null, stock, true, null);
        MvcResult productResult = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(productResult.getResponse().getContentAsString(), ProductResponse.class);
    }
}
