package congtuong.dev.mini_7eleven.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Comma-separated list or YAML list of allowed origins.
     */
    private List<String> allowedOrigins = new ArrayList<>();

    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private List<String> allowedHeaders = List.of("*");

    private List<String> exposedHeaders = List.of("Authorization");

    private boolean allowCredentials = true;

    /**
     * Max age (seconds) for preflight cache.
     */
    private long maxAge = 3600;
}

