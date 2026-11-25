package com.novel.platform.modules.user.config;

import com.novel.platform.modules.user.security.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserSecurityConfig {

    @Bean
    public JwtProperties jwtProperties(
            @Value("${JWT_SECRET:novel-platform-local-dev-secret-change-me-2026}") String secret,
            @Value("${JWT_EXPIRE_SECONDS:86400}") long expirationSeconds) {
        return new JwtProperties(secret, expirationSeconds);
    }
}
