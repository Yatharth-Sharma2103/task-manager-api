package org.example.taskmanager.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code app.jwt.*} configuration keys.
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Base64/plain secret used to sign tokens (must be at least 256 bits for HS256). */
    private String secret;

    /** Access-token time-to-live in milliseconds. */
    private long expirationMs = 86_400_000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
