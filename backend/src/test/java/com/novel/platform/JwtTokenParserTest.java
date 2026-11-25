package com.novel.platform;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.security.JwtProperties;
import com.novel.platform.common.security.JwtTokenProvider;
import com.novel.platform.modules.novel.auth.AuthorPrincipal;
import com.novel.platform.modules.novel.auth.JwtTokenParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenParserTest {

    private static final String SECRET = "test-secret-for-shared-a-b-jwt-validation-20261125";

    @Test
    void shouldAcceptAuthorTokenIssuedBySharedProvider() {
        JwtTokenProvider provider = provider(SECRET);
        JwtTokenParser parser = new JwtTokenParser(provider);
        String token = provider.createToken(202L, "author", "AUTHOR");

        AuthorPrincipal principal = parser.parse("Bearer " + token);

        assertEquals(202L, principal.userId());
        assertEquals("AUTHOR", principal.role());
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        JwtTokenProvider issuer = provider(SECRET);
        JwtTokenParser parser = new JwtTokenParser(provider(
                "another-test-secret-for-a-b-jwt-validation-20261125"));
        String token = issuer.createToken(202L, "author", "AUTHOR");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> parser.parse("Bearer " + token));

        assertEquals(40100, exception.getCode());
    }

    private JwtTokenProvider provider(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setExpireMinutes(30);
        return new JwtTokenProvider(properties);
    }
}
