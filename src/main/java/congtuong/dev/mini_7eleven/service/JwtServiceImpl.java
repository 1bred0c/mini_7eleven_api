package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.config.JwtProperties;
import congtuong.dev.mini_7eleven.pojo.Account;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is missing (app.jwt.secret)");
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 requires at least 256 bits (32 bytes)
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret is too short. It must be at least 32 bytes for HS256.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    @Override
    public String generateAccessToken(Account account) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiry = new Date(now + jwtProperties.getExpiration());

        Map<String, Object> claims = new HashMap<>();
        // Note: Map.of(...) does not allow null values and will throw NPE.
        claims.put("id", account.getId());
        if (account.getRole() != null) {
            claims.put("role", account.getRole().name());
        }
        if (account.getFullName() != null) {
            claims.put("fullName", account.getFullName());
        }

        return Jwts.builder()
                .subject(account.getEmail())
                .issuedAt(issuedAt)
                .expiration(expiry)
                .claims(claims)
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        JwtParser parser = Jwts.parser()
                .verifyWith(signingKey())
                .build();
        return parser.parseSignedClaims(token).getPayload();
    }
}

