package congtuong.dev.mini_7eleven.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Secret for signing access tokens (HS256).
     */
    private String secret;

    /**
     * Access token expiration in milliseconds.
     */
    private long expiration;

    /**
     * Refresh token expiration in milliseconds.
     */
    private long refreshExpiration;

    /**
     * Authorization header prefix.
     */
    private String tokenPrefix = "Bearer ";

    private String header = "Authorization";
}
