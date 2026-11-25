package com.novel.platform.modules.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new ObjectMapper(),
            new JwtProperties("test-secret-for-novel-platform-2026", 3600L)
    );

    @Test
    void shouldCreateAndParseToken() {
        String token = jwtService.createToken(12L, "reader01", "READER");
        JwtUser user = jwtService.parse(token);

        assertEquals(12L, user.userId());
        assertEquals("reader01", user.username());
        assertEquals("READER", user.role());
        assertEquals(3600L, jwtService.expiresInSeconds());
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.createToken(12L, "reader01", "READER");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(IllegalArgumentException.class, () -> jwtService.parse(tampered));
    }
}

