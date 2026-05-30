package congtuong.dev.mini_7eleven.integration;

import congtuong.dev.mini_7eleven.dto.LoginRequest;
import congtuong.dev.mini_7eleven.dto.LoginResponse;
import congtuong.dev.mini_7eleven.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        LoginResponse response = registerUser("user1@example.com", "Password123", "User One");

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void shouldLoginUserSuccessfully() throws Exception {
        registerUser("user2@example.com", "Password123", "User Two");

        LoginResponse response = login("user2@example.com", "Password123");

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
    }

    @Test
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        registerUser("user3@example.com", "Password123", "User Three");

        LoginRequest request = LoginRequest.builder()
                .email("user3@example.com")
                .password("WrongPass")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void shouldRejectDuplicateRegistration() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("User Four")
                .email("user4@example.com")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_EMAIL_EXISTS"));
    }
}

