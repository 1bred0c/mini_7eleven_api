package congtuong.dev.mini_7eleven.integration;

import congtuong.dev.mini_7eleven.dto.AddressResponse;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddressIntegrationTest extends IntegrationTestBase {

    @Test
    void shouldCreateListAndSetDefaultAddress() throws Exception {
        registerUser("addr1@example.com", "Password123", "User One");
        String userToken = loginAndGetToken("addr1@example.com", "Password123");

        AddressResponse address = createAddress(userToken, true);

        mockMvc.perform(get("/api/v1/addresses")
                        .header("Authorization", authHeader(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(address.getId()));

        mockMvc.perform(patch("/api/v1/addresses/{id}/default", address.getId())
                        .header("Authorization", authHeader(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));

        assertTrue(addressRepository.findById(address.getId()).orElseThrow().getIsDefault());
    }

    @Test
    void shouldRejectAccessToAnotherUsersAddress() throws Exception {
        registerUser("addr2@example.com", "Password123", "User Two");
        registerUser("addr3@example.com", "Password123", "User Three");

        String userOneToken = loginAndGetToken("addr2@example.com", "Password123");
        String userTwoToken = loginAndGetToken("addr3@example.com", "Password123");

        AddressResponse address = createAddress(userOneToken, true);

        mockMvc.perform(get("/api/v1/addresses/{id}", address.getId())
                        .header("Authorization", authHeader(userTwoToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADDRESS_NOT_FOUND"));
    }
}

