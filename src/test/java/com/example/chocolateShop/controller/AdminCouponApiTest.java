package com.example.chocolateShop.controller;

import com.example.chocolateShop.dtos.coupon.CouponRequest;
import com.example.chocolateShop.dtos.coupon.CouponResponse;
import com.example.chocolateShop.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminCouponApiTest extends BaseIntegrationTest {

    private static final String URL = "/api/v1/admin/coupons";

    @Test
    @DisplayName("POST /admin/coupons - no token returns 401")
    void create_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(percentCoupon("X", new BigDecimal("10")))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /admin/coupons - providing both percent and amount is rejected")
    void create_withBothDiscountTypes_returnsBadRequest() throws Exception {
        String adminToken = createAdminAndGetToken();
        CouponRequest request = new CouponRequest(
                "BADCOUPON", new BigDecimal("10"), new BigDecimal("5"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(10), null, true);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /admin/coupons - providing neither percent nor amount is rejected")
    void create_withNeitherDiscountType_returnsBadRequest() throws Exception {
        String adminToken = createAdminAndGetToken();
        CouponRequest request = new CouponRequest(
                "EMPTYCOUPON", null, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(10), null, true);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /admin/coupons - validUntil before validFrom is rejected")
    void create_withInvalidDateRange_returnsBadRequest() throws Exception {
        String adminToken = createAdminAndGetToken();
        CouponRequest request = new CouponRequest(
                "BACKWARDS", new BigDecimal("10"), null,
                LocalDateTime.now(), LocalDateTime.now().minusDays(1), null, true);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /admin/coupons - creates successfully and normalizes code to uppercase")
    void create_succeeds_andUppercasesCode() throws Exception {
        String adminToken = createAdminAndGetToken();
        CouponRequest request = percentCoupon("save20", new BigDecimal("20"));

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SAVE20"))
                .andExpect(jsonPath("$.timesUsed").value(0))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /admin/coupons - duplicate code returns 409")
    void create_withDuplicateCode_returnsConflict() throws Exception {
        String adminToken = createAdminAndGetToken();
        CouponRequest request = percentCoupon("DUPCODE", new BigDecimal("15"));

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /admin/coupons/{id} - updates the discount value")
    void update_changesDiscountValue() throws Exception {
        String adminToken = createAdminAndGetToken();
        CouponResponse created = createCoupon(adminToken, percentCoupon("UPDATEME", new BigDecimal("5")));

        CouponRequest updateRequest = percentCoupon("UPDATEME", new BigDecimal("25"));

        mockMvc.perform(put(URL + "/" + created.id())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountPercent").value(25));
    }

    @Test
    @DisplayName("DELETE /admin/coupons/{id} - removes it")
    void delete_removesCoupon() throws Exception {
        String adminToken = createAdminAndGetToken();
        CouponResponse created = createCoupon(adminToken, percentCoupon("DELETEME", new BigDecimal("5")));

        mockMvc.perform(delete(URL + "/" + created.id())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(URL).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private CouponRequest percentCoupon(String code, BigDecimal percent) {
        return new CouponRequest(code, percent, null,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30), null, true);
    }

    private CouponResponse createCoupon(String adminToken, CouponRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post(URL)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), CouponResponse.class);
    }
}
