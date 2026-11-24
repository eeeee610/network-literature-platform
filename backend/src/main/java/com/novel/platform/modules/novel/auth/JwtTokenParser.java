package com.novel.platform.modules.novel.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novel.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * 临时鉴权：使用 JDK 自带 HMAC-SHA256 解析 JWT（HS256）。
 * 仅取 sub=userId、role 声明，联调时整体替换为 A 模块的鉴权实现。
 */
@Component
public class JwtTokenParser {

    private static final String DEFAULT_SECRET = "novel-platform-dev-secret";
    private static final String BEARER_PREFIX = "bearer ";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] secret;

    public JwtTokenParser() {
        String envSecret = System.getenv("JWT_SECRET");
        String value = (envSecret == null || envSecret.isBlank()) ? DEFAULT_SECRET : envSecret;
        this.secret = value.getBytes(StandardCharsets.UTF_8);
    }

    public AuthorPrincipal parse(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw unauthorized();
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        String[] parts = token.split("\\.");
        if (token.isEmpty() || parts.length != 3) {
            throw unauthorized();
        }

        JsonNode header = readJson(parts[0]);
        if (!"HS256".equalsIgnoreCase(header.path("alg").asText())) {
            throw unauthorized();
        }

        byte[] expected = sign(parts[0] + "." + parts[1]);
        byte[] actual = decodeBase64(parts[2]);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw unauthorized();
        }

        JsonNode payload = readJson(parts[1]);
        long now = Instant.now().getEpochSecond();
        JsonNode exp = payload.get("exp");
        if (exp != null && exp.isNumber() && now >= exp.asLong()) {
            throw unauthorized();
        }
        JsonNode nbf = payload.get("nbf");
        if (nbf != null && nbf.isNumber() && now < nbf.asLong()) {
            throw unauthorized();
        }

        String subject = text(payload, "sub");
        if (subject == null) {
            subject = text(payload, "userId");
        }
        if (subject == null) {
            throw unauthorized();
        }
        long userId;
        try {
            userId = Long.parseLong(subject.trim());
        } catch (NumberFormatException exception) {
            throw unauthorized();
        }

        String role = text(payload, "role");
        if (role == null) {
            throw unauthorized();
        }
        return new AuthorPrincipal(userId, role);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private JsonNode readJson(String base64Url) {
        try {
            return objectMapper.readTree(decodeBase64(base64Url));
        } catch (Exception exception) {
            throw unauthorized();
        }
    }

    private byte[] decodeBase64(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw unauthorized();
        }
    }

    private byte[] sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw unauthorized();
        }
    }

    private BusinessException unauthorized() {
        return new BusinessException(40100, "未登录或登录已失效", HttpStatus.UNAUTHORIZED);
    }
}
