package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.category.CategoryRequest;
import com.example.chocolateShop.dtos.category.CategoryResponse;
import com.example.chocolateShop.dtos.product.ProductRequest;
import com.example.chocolateShop.dtos.product.ProductResponse;
import com.example.chocolateShop.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductApiTest extends BaseIntegrationTest {

    private static final String PUBLIC_URL = "/api/v1/products";
    private static final String ADMIN_URL = "/api/v1/admin/products";
    private static final String ADMIN_CATEGORY_URL = "/api/v1/admin/categories";

    @Test
    @DisplayName("GET /products - unknown slug returns 404")
    void getBySlug_unknown_returnsNotFound() throws Exception {
        mockMvc.perform(get(PUBLIC_URL + "/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /admin/products - admin creates product without images")
    void create_asAdmin_createsProductWithoutImages() throws Exception {
        String adminToken = createAdminAndGetToken();
        Long categoryId = createCategory(adminToken, "Dark Chocolate").id();

        ProductRequest request = new ProductRequest(
                categoryId, "70% Dark Bar", "Intense and bold", new BigDecimal("12.50"), null,
                50, true);

        mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("70% Dark Bar"))
                .andExpect(jsonPath("$.slug").value("70-dark-bar"))
                .andExpect(jsonPath("$.effectivePrice").value(12.50))
                .andExpect(jsonPath("$.images.length()").value(0));
    }

    @Test
    @DisplayName("POST /admin/products - unknown category returns 404")
    void create_withUnknownCategory_returnsNotFound() throws Exception {
        String adminToken = createAdminAndGetToken();
        ProductRequest request = new ProductRequest(
                999L, "Ghost Product", null, new BigDecimal("5.00"), null, 10, true);

        mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Discount price is used as the effective price when present")
    void effectivePrice_usesDiscountPriceWhenSet() throws Exception {
        String adminToken = createAdminAndGetToken();
        Long categoryId = createCategory(adminToken, "Milk Chocolate").id();

        ProductResponse created = createProduct(adminToken, categoryId, "Milk Bar",
                new BigDecimal("10.00"), new BigDecimal("7.50"), 20);

        mockMvc.perform(get(PUBLIC_URL + "/" + created.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(10.00))
                .andExpect(jsonPath("$.discountPrice").value(7.50))
                .andExpect(jsonPath("$.effectivePrice").value(7.50));
    }

    @Test
    @DisplayName("Inactive products are hidden from the public listing but visible to admin")
    void inactiveProduct_hiddenFromPublicListing() throws Exception {
        String adminToken = createAdminAndGetToken();
        Long categoryId = createCategory(adminToken, "Seasonal").id();

        ProductRequest request = new ProductRequest(
                categoryId, "Discontinued Bar", null, new BigDecimal("5.00"), null, 0, false);

        mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(PUBLIC_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(get(ADMIN_URL)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    @DisplayName("Cursor pagination (NEWEST) walks the full set with no duplicates or gaps")
    void listNewest_cursorPagination_returnsAllItemsExactlyOnce() throws Exception {
        String adminToken = createAdminAndGetToken();
        Long categoryId = createCategory(adminToken, "Assorted").id();

        for (int i = 1; i <= 25; i++) {
            createProduct(adminToken, categoryId, "Product " + i,
                    new BigDecimal("10.00"), null, 100);
        }

        Set<Long> seenIds = new HashSet<>();
        String cursor = null;
        int pageCount = 0;

        do {
            String url = PUBLIC_URL + "?limit=10" + (cursor != null ? "&cursor=" + cursor : "");
            MvcResult result = mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andReturn();

            var page = objectMapper.readTree(result.getResponse().getContentAsString());
            for (var item : page.get("items")) {
                boolean isNew = seenIds.add(item.get("id").asLong());
                assertThat(isNew).as("product id should not repeat across pages").isTrue();
            }

            cursor = page.get("hasNext").asBoolean() ? page.get("nextCursor").asText() : null;
            pageCount++;
            assertThat(pageCount).as("safety valve against infinite loop").isLessThan(10);
        } while (cursor != null);

        assertThat(seenIds).hasSize(25);
        assertThat(pageCount).isEqualTo(3); // 10 + 10 + 5
    }

    @Test
    @DisplayName("Cursor pagination (PRICE_ASC) returns items in ascending effective-price order")
    void listPriceAsc_returnsItemsInPriceOrder() throws Exception {
        String adminToken = createAdminAndGetToken();
        Long categoryId = createCategory(adminToken, "Priced").id();

        createProduct(adminToken, categoryId, "Cheap Bar", new BigDecimal("3.00"), null, 10);
        createProduct(adminToken, categoryId, "Mid Bar", new BigDecimal("8.00"), null, 10);
        createProduct(adminToken, categoryId, "Pricey Bar", new BigDecimal("20.00"), null, 10);
        // discount makes this one actually the cheapest
        createProduct(adminToken, categoryId, "Discounted Bar", new BigDecimal("15.00"), new BigDecimal("1.00"), 10);

        MvcResult result = mockMvc.perform(get(PUBLIC_URL + "?sort=PRICE_ASC&limit=10"))
                .andExpect(status().isOk())
                .andReturn();

        var page = objectMapper.readTree(result.getResponse().getContentAsString());
        List<String> namesInOrder = page.get("items").findValuesAsText("name");

        assertThat(namesInOrder).containsExactly("Discounted Bar", "Cheap Bar", "Mid Bar", "Pricey Bar");
    }

    @Test
    @DisplayName("Filtering by category only returns matching products")
    void listFilteredByCategory_returnsOnlyMatchingProducts() throws Exception {
        String adminToken = createAdminAndGetToken();
        Long chocolateId = createCategory(adminToken, "Chocolate Bars").id();
        Long giftId = createCategory(adminToken, "Gift Boxes").id();

        createProduct(adminToken, chocolateId, "Bar One", new BigDecimal("5.00"), null, 10);
        createProduct(adminToken, giftId, "Box One", new BigDecimal("25.00"), null, 10);

        mockMvc.perform(get(PUBLIC_URL + "?categoryId=" + chocolateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Bar One"));
    }

    @Test
    @DisplayName("Filtering by keyword matches product names case-insensitively")
    void listFilteredByKeyword_matchesCaseInsensitively() throws Exception {
        String adminToken = createAdminAndGetToken();
        Long categoryId = createCategory(adminToken, "Search Test").id();

        createProduct(adminToken, categoryId, "Hazelnut Truffle", new BigDecimal("6.00"), null, 10);
        createProduct(adminToken, categoryId, "Vanilla Bar", new BigDecimal("6.00"), null, 10);

        mockMvc.perform(get(PUBLIC_URL + "?keyword=hazel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Hazelnut Truffle"));
    }

    @Test
    @DisplayName("PUT /admin/products/{id} - update changes fields and slug")
    void update_changesFieldsAndSlug() throws Exception {
        String adminToken = createAdminAndGetToken();
        Long categoryId = createCategory(adminToken, "Editable").id();
        ProductResponse created = createProduct(adminToken, categoryId, "Old Name",
                new BigDecimal("9.00"), null, 5);

        ProductRequest updateRequest = new ProductRequest(
                categoryId, "New Name", "New description", new BigDecimal("11.00"),
                new BigDecimal("9.99"), 15, true);

        mockMvc.perform(put(ADMIN_URL + "/" + created.id())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.slug").value("new-name"))
                .andExpect(jsonPath("$.price").value(11.00))
                .andExpect(jsonPath("$.stockQuantity").value(15));
    }

    @Test
    @DisplayName("DELETE /admin/products/{id} - removes it, subsequent GET is 404")
    void delete_removesProduct() throws Exception {
        String adminToken = createAdminAndGetToken();
        Long categoryId = createCategory(adminToken, "Deletable").id();
        ProductResponse created = createProduct(adminToken, categoryId, "Gone Soon",
                new BigDecimal("4.00"), null, 5);

        mockMvc.perform(delete(ADMIN_URL + "/" + created.id())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PUBLIC_URL + "/" + created.slug()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /admin/products/{id}/images then DELETE - add and remove an image")
    void addAndRemoveImage() throws Exception {
        String adminToken = createAdminAndGetToken();
        Long categoryId = createCategory(adminToken, "Imagery").id();
        ProductResponse created = createProduct(adminToken, categoryId, "Bare Bar",
                new BigDecimal("4.00"), null, 5);

        MockMultipartFile imageFile = new MockMultipartFile(
                "file", "tiny.png", "image/png",
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADUlEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC"));

        MvcResult addResult = mockMvc.perform(multipart(ADMIN_URL + "/" + created.id() + "/images")
                        .file(imageFile)
                        .param("primary", "true")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images.length()").value(1))
                .andReturn();

        ProductResponse withImage = objectMapper.readValue(
                addResult.getResponse().getContentAsString(), ProductResponse.class);
        Long imageId = withImage.images().get(0).id();

        mockMvc.perform(delete(ADMIN_URL + "/" + created.id() + "/images/" + imageId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(0));
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private CategoryResponse createCategory(String adminToken, String name) throws Exception {
        var request = new CategoryRequest(name, null);
        MvcResult result = mockMvc.perform(post(ADMIN_CATEGORY_URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), CategoryResponse.class);
    }

    private ProductResponse createProduct(String adminToken, Long categoryId, String name,
                                           BigDecimal price, BigDecimal discountPrice, int stock) throws Exception {
        ProductRequest request = new ProductRequest(
                categoryId, name, "Description for " + name, price, discountPrice, stock, true);

        MvcResult result = mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), ProductResponse.class);
    }
}
