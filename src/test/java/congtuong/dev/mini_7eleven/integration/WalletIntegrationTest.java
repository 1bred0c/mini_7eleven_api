package congtuong.dev.mini_7eleven.integration;

import congtuong.dev.mini_7eleven.dto.*;
import congtuong.dev.mini_7eleven.enums.PaymentMethod;
import congtuong.dev.mini_7eleven.enums.ProductStatus;
import congtuong.dev.mini_7eleven.pojo.Wallet;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletIntegrationTest extends IntegrationTestBase {

    @Test
    void shouldTopUpWalletAndPayForOrder() throws Exception {
        createAdminAccountIfNeeded("admin-wallet@example.com", "AdminPass123");
        String adminToken = loginAndGetToken("admin-wallet@example.com", "AdminPass123");

        CategoryResponse category = createCategory(adminToken, "Dairy");
        ProductResponse product = createProduct(adminToken, category.getId(), "Milk", BigDecimal.valueOf(20), 10, ProductStatus.ACTIVE);

        registerUser("wallet-user@example.com", "Password123", "Wallet User");
        String userToken = loginAndGetToken("wallet-user@example.com", "Password123");

        AddressResponse address = createAddress(userToken, true);

        WalletTopUpRequest topUpRequest = WalletTopUpRequest.builder()
                .amount(BigDecimal.valueOf(100))
                .description("Initial top up")
                .build();

        mockMvc.perform(post("/api/v1/wallets/me/top-up")
                        .header("Authorization", authHeader(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topUpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100));

        OrderResponse order = createOrder(userToken, address.getId(), PaymentMethod.WALLET,
                List.of(OrderItemRequest.builder().productId(product.getId()).quantity(2).build()));

        assertEquals("PAID", order.getPaymentStatus().name());

        Wallet wallet = walletRepository.findByAccountId(accountRepository.findByEmail("wallet-user@example.com").orElseThrow().getId())
                .orElseThrow();
        assertEquals(BigDecimal.valueOf(60), wallet.getBalance());

        int transactionCount = walletTransactionRepository.findByWalletId(wallet.getId(), PageRequest.of(0, 10)).getContent().size();
        assertEquals(2, transactionCount);
    }
}

