package com.novel.platform.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 签发与校验：sub 为 userId，载荷包含 username 与 role。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final Duration expire;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire-hours:12}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = Duration.ofHours(expireHours);
    }

    public String generate(Long userId, String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expire)))
                .signWith(key)
                .compact();
    }

    public long expiresInSeconds() {
        return expire.toSeconds();
    }

    /**
     * 校验并解析令牌，失败时返回 null，由调用方决定错误响应。
     */
    public AuthUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            if (username == null || role == null) {
                return null;
            }
            return new AuthUser(userId, username, role);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
