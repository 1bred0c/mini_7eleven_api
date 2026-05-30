package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.*;
import congtuong.dev.mini_7eleven.enums.Role;
import congtuong.dev.mini_7eleven.exception.ConflictException;
import congtuong.dev.mini_7eleven.exception.UnauthorizedException;
import congtuong.dev.mini_7eleven.pojo.Account;
import congtuong.dev.mini_7eleven.pojo.RefreshToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AccountService accountService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldRegisterAccountSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe")
                .email("  JANE@EXAMPLE.COM ")
                .password("password123")
                .build();

        when(accountService.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        Account saved = buildAccount("jane@example.com");
        saved.setId(10L);
        when(accountService.create(any(Account.class))).thenReturn(saved);
        when(jwtService.generateAccessToken(saved)).thenReturn("access-token");
        when(refreshTokenService.issueRefreshToken(10L)).thenReturn("refresh-token");

        LoginResponse response = authService.register(request);

        assertEquals("Bearer", response.getTokenType());
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountService).create(captor.capture());
        Account created = captor.getValue();
        assertEquals("jane@example.com", created.getEmail());
        assertEquals("hashed", created.getPasswordHash());
        assertEquals(Role.USER, created.getRole());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe")
                .email("jane@example.com")
                .password("password123")
                .build();

        when(accountService.existsByEmail("jane@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(accountService, never()).create(any(Account.class));
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("jane@example.com")
                .password("password123")
                .build();

        Account account = buildAccount("jane@example.com");
        account.setId(20L);
        account.setPasswordHash("hashed");

        when(accountService.findByEmail("jane@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(account)).thenReturn("access-token");
        when(refreshTokenService.issueRefreshToken(20L)).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertEquals("Bearer", response.getTokenType());
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void shouldRejectLoginWithInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("jane@example.com")
                .password("wrong")
                .build();

        Account account = buildAccount("jane@example.com");
        account.setPasswordHash("hashed");

        when(accountService.findByEmail("jane@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
        verify(jwtService, never()).generateAccessToken(any(Account.class));
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        Account account = buildAccount("jane@example.com");
        account.setId(30L);

        RefreshToken refreshToken = RefreshToken.builder()
                .account(account)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenService.validateRefreshToken("old-token")).thenReturn(refreshToken);
        when(jwtService.generateAccessToken(account)).thenReturn("new-access");
        when(refreshTokenService.issueRefreshToken(30L)).thenReturn("new-refresh");

        LoginResponse response = authService.refresh(RefreshTokenRequest.builder()
                .refreshToken("old-token")
                .build());

        verify(refreshTokenService).revokeRefreshToken("old-token");
        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
    }

    @Test
    void shouldRevokeRefreshTokenOnLogout() {
        authService.logout(RefreshTokenRequest.builder().refreshToken("logout-token").build());
        verify(refreshTokenService).revokeRefreshToken("logout-token");
    }

    private Account buildAccount(String email) {
        return Account.builder()
                .fullName("Jane Doe")
                .email(email)
                .passwordHash("hashed")
                .role(Role.USER)
                .build();
    }
}

