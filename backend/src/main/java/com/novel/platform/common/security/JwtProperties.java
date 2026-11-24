package com.novel.platform.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Long expirationSeconds) {

    public String effectiveSecret() {
        if (secret == null || secret.isBlank()) {
            return "novel-platform-local-dev-secret-change-me-2026";
        }
        return secret;
    }

    public long effectiveExpirationSeconds() {
        if (expirationSeconds == null || expirationSeconds <= 0) {
            return 86_400L;
        }
        return expirationSeconds;
    }
}
