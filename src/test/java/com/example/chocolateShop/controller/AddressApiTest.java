//package com.example.chocolateShop.controller;
//
//
//import com.example.chocolateShop.dtos.auth.AuthResponse;
//import com.example.chocolateShop.dtos.address.AddressRequest;
//import com.example.chocolateShop.dtos.address.AddressResponse;
//import com.example.chocolateShop.support.BaseIntegrationTest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MvcResult;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//class AddressApiTest extends BaseIntegrationTest {
//
//    private static final String URL = "/api/v1/addresses";
//
//    @Test
//    @DisplayName("GET /addresses - no token returns 401")
//    void getAll_withoutAuth_returnsUnauthorized() throws Exception {
//        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    @DisplayName("POST /addresses - first address for a user automatically becomes default")
//    void create_firstAddress_becomesDefault() throws Exception {
//        AuthResponse auth = registerCustomer();
//        AddressRequest request = homeAddress();
//
//        mockMvc.perform(post(URL)
//                        .header("Authorization", bearer(auth.accessToken()))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.isDefault").value(true));
//    }
//
//    @Test
//    @DisplayName("POST /addresses - second address does not override default unless requested")
//    void create_secondAddress_doesNotOverrideDefaultByDefault() throws Exception {
//        AuthResponse auth = registerCustomer();
//        createAddress(auth.accessToken(), homeAddress());
//        AddressResponse second = createAddress(auth.accessToken(), officeAddress());
//
//        assertDefaultFlagIs(second.id(), false, auth.accessToken());
//    }
//
//    @Test
//    @DisplayName("POST /addresses - explicitly setting isDefault=true reassigns the default")
//    void create_withIsDefaultTrue_reassignsDefault() throws Exception {
//        AuthResponse auth = registerCustomer();
//        AddressResponse first = createAddress(auth.accessToken(), homeAddress());
//
//        AddressRequest secondRequest = new AddressRequest(
//                "Office", "Test Customer", "+8801700000001", "456 Office Rd", null,
//                "Dhaka", "Dhaka", "1212", "Bangladesh", true);
//        AddressResponse second = createAddress(auth.accessToken(), secondRequest);
//
//        assertDefaultFlagIs(first.id(), false, auth.accessToken());
//        assertDefaultFlagIs(second.id(), true, auth.accessToken());
//    }
//
//    @Test
//    @DisplayName("PUT /addresses/{id} - updates fields")
//    void update_changesFields() throws Exception {
//        AuthResponse auth = registerCustomer();
//        AddressResponse created = createAddress(auth.accessToken(), homeAddress());
//
//        AddressRequest updateRequest = new AddressRequest(
//                "Home (updated)", "Updated Name", "+8801700000099", "999 New Rd", "Apt 2",
//                "Chattogram", null, "4000", "Bangladesh", null);
//
//        mockMvc.perform(put(URL + "/" + created.id())
//                        .header("Authorization", bearer(auth.accessToken()))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(updateRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.recipientName").value("Updated Name"))
//                .andExpect(jsonPath("$.city").value("Chattogram"));
//    }
//
//    @Test
//    @DisplayName("A user cannot view, update, or delete another user's address")
//    void ownership_isEnforced() throws Exception {
//        AuthResponse ownerAuth = registerCustomer();
//        AuthResponse strangerAuth = registerCustomer();
//        AddressResponse ownerAddress = createAddress(ownerAuth.accessToken(), homeAddress());
//
//        mockMvc.perform(put(URL + "/" + ownerAddress.id())
//                        .header("Authorization", bearer(strangerAuth.accessToken()))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(homeAddress())))
//                .andExpect(status().isNotFound());
//
//        mockMvc.perform(delete(URL + "/" + ownerAddress.id())
//                        .header("Authorization", bearer(strangerAuth.accessToken())))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @DisplayName("DELETE /addresses/{id} - removes it from the user's list")
//    void delete_removesAddress() throws Exception {
//        AuthResponse auth = registerCustomer();
//        AddressResponse created = createAddress(auth.accessToken(), homeAddress());
//
//        mockMvc.perform(delete(URL + "/" + created.id())
//                        .header("Authorization", bearer(auth.accessToken())))
//                .andExpect(status().isNoContent());
//
//        mockMvc.perform(get(URL)
//                        .header("Authorization", bearer(auth.accessToken())))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(0));
//    }
//
//    // ---------------------------------------------------------
//    // Helpers
//    // ---------------------------------------------------------
//
//    private AddressRequest homeAddress() {
//        return new AddressRequest("Home", "Test Customer", "+8801700000000",
//                "123 Home St", null, "Dhaka", "Dhaka", "1207", "Bangladesh", null);
//    }
//
//    private AddressRequest officeAddress() {
//        return new AddressRequest("Office", "Test Customer", "+8801700000001",
//                "456 Office Rd", null, "Dhaka", "Dhaka", "1212", "Bangladesh", null);
//    }
//
//    private AddressResponse createAddress(String token, AddressRequest request) throws Exception {
//        MvcResult result = mockMvc.perform(post(URL)
//                        .header("Authorization", bearer(token))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andReturn();
//        return objectMapper.readValue(result.getResponse().getContentAsString(), AddressResponse.class);
//    }
//
//    private void assertDefaultFlagIs(Long addressId, boolean expected, String token) throws Exception {
//        MvcResult result = mockMvc.perform(get(URL).header("Authorization", bearer(token)))
//                .andExpect(status().isOk())
//                .andReturn();
//        var list = objectMapper.readTree(result.getResponse().getContentAsString());
//        for (var node : list) {
//            if (node.get("id").asLong() == addressId) {
//                org.assertj.core.api.Assertions.assertThat(node.get("isDefault").asBoolean()).isEqualTo(expected);
//                return;
//            }
//        }
//        throw new AssertionError("Address " + addressId + " not found in list");
//    }
//}
