package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.category.CategoryRequest;
import com.example.chocolateShop.dtos.category.CategoryResponse;
import com.example.chocolateShop.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CategoryApiTest extends BaseIntegrationTest {

    private static final String PUBLIC_URL = "/api/v1/categories";
    private static final String ADMIN_URL = "/api/v1/admin/categories";

    @Test
    @DisplayName("GET /categories - public, returns empty list when none exist")
    void getAll_public_returnsEmptyList() throws Exception {
        mockMvc.perform(get(PUBLIC_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /categories/{slug} - unknown slug returns 404")
    void getBySlug_unknown_returnsNotFound() throws Exception {
        mockMvc.perform(get(PUBLIC_URL + "/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /admin/categories - admin can create, slug auto-generated")
    void create_asAdmin_createsWithSlug() throws Exception {
        String adminToken = createAdminAndGetToken();
        CategoryRequest request = new CategoryRequest("Dark Chocolate", "Rich and bold");

        mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Dark Chocolate"))
                .andExpect(jsonPath("$.slug").value("dark-chocolate"))
                .andExpect(jsonPath("$.id", notNullValue()));

        // now visible on the public listing
        mockMvc.perform(get(PUBLIC_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Dark Chocolate"));
    }

    @Test
    @DisplayName("POST /admin/categories - duplicate name returns 409")
    void create_withDuplicateName_returnsConflict() throws Exception {
        String adminToken = createAdminAndGetToken();
        CategoryRequest request = new CategoryRequest("Milk Chocolate", null);

        mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /admin/categories - no token returns 401")
    void create_withoutAuth_returnsUnauthorized() throws Exception {
        CategoryRequest request = new CategoryRequest("Gift Box", null);

        mockMvc.perform(post(ADMIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /admin/categories - customer token returns 403")
    void create_asCustomer_returnsForbidden() throws Exception {
        var customerAuth = registerCustomer();
        CategoryRequest request = new CategoryRequest("Gift Box", null);

        mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", bearer(customerAuth.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /admin/categories/{id} - renames and regenerates slug")
    void update_changesNameAndSlug() throws Exception {
        String adminToken = createAdminAndGetToken();
        CategoryResponse created = createCategory(adminToken, "White Chocolate", null);

        CategoryRequest updateRequest = new CategoryRequest("Premium White Chocolate", "Updated description");

        mockMvc.perform(put(ADMIN_URL + "/" + created.id())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Premium White Chocolate"))
                .andExpect(jsonPath("$.slug").value("premium-white-chocolate"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @DisplayName("DELETE /admin/categories/{id} - removes it, subsequent GET is 404")
    void delete_removesCategory() throws Exception {
        String adminToken = createAdminAndGetToken();
        CategoryResponse created = createCategory(adminToken, "Truffles", null);

        mockMvc.perform(delete(ADMIN_URL + "/" + created.id())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PUBLIC_URL + "/" + created.slug()))
                .andExpect(status().isNotFound());
    }

    protected CategoryResponse createCategory(String adminToken, String name, String description) throws Exception {
        CategoryRequest request = new CategoryRequest(name, description);
        MvcResult result = mockMvc.perform(post(ADMIN_URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), CategoryResponse.class);
    }
}
