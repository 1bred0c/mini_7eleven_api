package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.config.JwtProperties;
import congtuong.dev.mini_7eleven.exception.NotFoundException;
import congtuong.dev.mini_7eleven.exception.UnauthorizedException;
import congtuong.dev.mini_7eleven.pojo.Account;
import congtuong.dev.mini_7eleven.pojo.RefreshToken;
import congtuong.dev.mini_7eleven.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountService accountService;
    private final JwtProperties jwtProperties;

    @Override
    public String issueRefreshToken(Long accountId) {
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new NotFoundException("ACCOUNT_NOT_FOUND", "Account not found"));

        String raw = generateRawToken();
        String hash = sha256Hex(raw);

        RefreshToken refreshToken = RefreshToken.builder()
                .account(account)
                .tokenHash(hash)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtProperties.getRefreshExpiration())))
                .build();

        refreshTokenRepository.save(refreshToken);
        return raw;
    }

    @Override
    public RefreshToken validateRefreshToken(String rawRefreshToken) {
        String hash = sha256Hex(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("AUTH_INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        LocalDateTime now = LocalDateTime.now();
        if (token.isRevoked()) {
            throw new UnauthorizedException("AUTH_REFRESH_TOKEN_REVOKED", "Refresh token revoked");
        }
        if (token.isExpired(now)) {
            throw new UnauthorizedException("AUTH_REFRESH_TOKEN_EXPIRED", "Refresh token expired");
        }
        return token;
    }

    @Override
    public void revokeRefreshToken(String rawRefreshToken) {
        String hash = sha256Hex(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    private String generateRawToken() {
        // 43+ chars base64url from random UUID + extra entropy
        String value = UUID.randomUUID() + ":" + UUID.randomUUID();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

