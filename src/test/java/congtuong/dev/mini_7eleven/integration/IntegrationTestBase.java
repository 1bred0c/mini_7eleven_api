package congtuong.dev.mini_7eleven.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import congtuong.dev.mini_7eleven.dto.*;
import congtuong.dev.mini_7eleven.enums.PaymentMethod;
import congtuong.dev.mini_7eleven.enums.ProductStatus;
import congtuong.dev.mini_7eleven.enums.Role;
import congtuong.dev.mini_7eleven.pojo.Account;
import congtuong.dev.mini_7eleven.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected AddressRepository addressRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected OrderItemRepository orderItemRepository;

    @Autowired
    protected WalletRepository walletRepository;

    @Autowired
    protected WalletTransactionRepository walletTransactionRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        walletTransactionRepository.deleteAll();
        walletRepository.deleteAll();
        addressRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        accountRepository.deleteAll();
    }

    protected LoginResponse registerUser(String email, String password, String fullName) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName(fullName)
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
    }

    protected LoginResponse login(String email, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
    }

    protected String loginAndGetToken(String email, String password) throws Exception {
        return login(email, password).getAccessToken();
    }

    protected void createAdminAccountIfNeeded(String email, String password) {
        if (accountRepository.findByEmail(email).isPresent()) {
            return;
        }

        Account admin = Account.builder()
                .fullName("Admin")
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .build();

        accountRepository.save(admin);
    }

    protected CategoryResponse createCategory(String adminToken, String name) throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name(name)
                .description("Category " + name)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), CategoryResponse.class);
    }

    protected ProductResponse createProduct(String adminToken,
                                            Long categoryId,
                                            String name,
                                            BigDecimal price,
                                            int stockQuantity,
                                            ProductStatus status) throws Exception {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name(name)
                .description("Product " + name)
                .price(price)
                .stockQuantity(stockQuantity)
                .imageUrl("https://example.com/img.png")
                .status(status)
                .categoryId(categoryId)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), ProductResponse.class);
    }

    protected AddressResponse createAddress(String userToken, boolean makeDefault) throws Exception {
        AddressRequest request = AddressRequest.builder()
                .receiverName("John Doe")
                .phoneNumber("0123456789")
                .addressLine("123 Main St")
                .ward("Ward 1")
                .district("District 1")
                .city("City")
                .isDefault(makeDefault)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/addresses")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), AddressResponse.class);
    }

    protected OrderResponse createOrder(String userToken,
                                        Long addressId,
                                        PaymentMethod paymentMethod,
                                        List<OrderItemRequest> items) throws Exception {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .addressId(addressId)
                .paymentMethod(paymentMethod)
                .items(items)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), OrderResponse.class);
    }

    protected String authHeader(String token) {
        return "Bearer " + token;
    }
}

