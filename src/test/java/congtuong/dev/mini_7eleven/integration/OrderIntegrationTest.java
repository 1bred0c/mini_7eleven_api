package congtuong.dev.mini_7eleven.integration;

import congtuong.dev.mini_7eleven.dto.*;
import congtuong.dev.mini_7eleven.enums.OrderStatus;
import congtuong.dev.mini_7eleven.enums.PaymentMethod;
import congtuong.dev.mini_7eleven.enums.ProductStatus;
import congtuong.dev.mini_7eleven.pojo.Product;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderIntegrationTest extends IntegrationTestBase {

    @Test
    void shouldCreateOrderReduceStockAndAllowAccess() throws Exception {
        createAdminAccountIfNeeded("admin-order@example.com", "AdminPass123");
        String adminToken = loginAndGetToken("admin-order@example.com", "AdminPass123");

        CategoryResponse category = createCategory(adminToken, "Drinks");
        ProductResponse product = createProduct(adminToken, category.getId(), "Tea", BigDecimal.valueOf(5), 10, ProductStatus.ACTIVE);

        registerUser("order-user@example.com", "Password123", "Order User");
        String userToken = loginAndGetToken("order-user@example.com", "Password123");

        AddressResponse address = createAddress(userToken, true);

        List<OrderItemRequest> items = List.of(
                OrderItemRequest.builder().productId(product.getId()).quantity(2).build()
        );

        OrderResponse response = createOrder(userToken, address.getId(), PaymentMethod.CASH_ON_DELIVERY, items);

        assertEquals(1, response.getItems().size());
        assertEquals(BigDecimal.valueOf(10), response.getTotalAmount());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(8, updated.getStockQuantity());

        mockMvc.perform(get("/api/v1/orders/my/{id}", response.getId())
                        .header("Authorization", authHeader(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId()));

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", authHeader(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        OrderStatusUpdateRequest statusUpdate = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        mockMvc.perform(patch("/api/v1/orders/{id}/status", response.getId())
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldRejectOtherUserAccessToOrder() throws Exception {
        createAdminAccountIfNeeded("admin-order2@example.com", "AdminPass123");
        String adminToken = loginAndGetToken("admin-order2@example.com", "AdminPass123");

        CategoryResponse category = createCategory(adminToken, "Snacks");
        ProductResponse product = createProduct(adminToken, category.getId(), "Cookie", BigDecimal.valueOf(3), 5, ProductStatus.ACTIVE);

        registerUser("order-user2@example.com", "Password123", "Order User Two");
        registerUser("order-user3@example.com", "Password123", "Order User Three");

        String userToken = loginAndGetToken("order-user2@example.com", "Password123");
        String otherToken = loginAndGetToken("order-user3@example.com", "Password123");

        AddressResponse address = createAddress(userToken, true);
        OrderResponse response = createOrder(userToken, address.getId(), PaymentMethod.CASH_ON_DELIVERY,
                List.of(OrderItemRequest.builder().productId(product.getId()).quantity(1).build()));

        mockMvc.perform(get("/api/v1/orders/my/{id}", response.getId())
                        .header("Authorization", authHeader(otherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));
    }
}

