package congtuong.dev.mini_7eleven.integration;

import congtuong.dev.mini_7eleven.dto.*;
import congtuong.dev.mini_7eleven.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductCategoryIntegrationTest extends IntegrationTestBase {

    @Test
    void shouldAllowAdminToManageProductsAndUsersToBrowse() throws Exception {
        createAdminAccountIfNeeded("admin@example.com", "AdminPass123");
        String adminToken = loginAndGetToken("admin@example.com", "AdminPass123");

        CategoryResponse category = createCategory(adminToken, "Snacks");
        ProductResponse product = createProduct(adminToken, category.getId(), "Chips", BigDecimal.valueOf(2.5), 20, ProductStatus.ACTIVE);

        registerUser("user@example.com", "Password123", "User");
        String userToken = loginAndGetToken("user@example.com", "Password123");

        mockMvc.perform(get("/api/v1/products")
                        .param("status", "ACTIVE")
                        .header("Authorization", authHeader(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(product.getId()));

        mockMvc.perform(get("/api/v1/products/{id}", product.getId())
                        .header("Authorization", authHeader(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chips"));

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .name("Potato Chips")
                .price(BigDecimal.valueOf(3.0))
                .build();

        mockMvc.perform(put("/api/v1/products/{id}", product.getId())
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Potato Chips"))
                .andExpect(jsonPath("$.price").value(3.0));

        ProductStatusUpdateRequest statusUpdate = ProductStatusUpdateRequest.builder()
                .status(ProductStatus.INACTIVE)
                .build();

        mockMvc.perform(patch("/api/v1/products/{id}/status", product.getId())
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/products")
                        .param("status", "ACTIVE")
                        .header("Authorization", authHeader(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(delete("/api/v1/products/{id}", product.getId())
                        .header("Authorization", authHeader(adminToken)))
                .andExpect(status().isOk());

        assertEquals(0, productRepository.count());
    }

    @Test
    void shouldRejectUserAccessToAdminEndpoints() throws Exception {
        registerUser("user2@example.com", "Password123", "User Two");
        String userToken = loginAndGetToken("user2@example.com", "Password123");

        CategoryRequest request = CategoryRequest.builder()
                .name("Beverages")
                .description("Drinks")
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", authHeader(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}



