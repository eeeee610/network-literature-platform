package com.novel.platform.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量 JWT（HS256）实现，避免为课程项目引入完整 Spring Security 过滤链。
 * 载荷只放 userId / username / role / iat / exp，不放敏感信息。
 */
@Service
public class JwtService {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final JwtProperties properties;
    private final SecretKeySpec key;

    public JwtService(ObjectMapper objectMapper, JwtProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.key = buildKey(properties.effectiveSecret());
    }

    public String createToken(Long userId, String username, String role) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + properties.effectiveExpirationSeconds();

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", String.valueOf(userId));
            payload.put("username", username);
            payload.put("role", role);
            payload.put("iat", now);
            payload.put("exp", exp);

            String headerPart = encode(objectMapper.writeValueAsBytes(header));
            String payloadPart = encode(objectMapper.writeValueAsBytes(payload));
            String signingInput = headerPart + "." + payloadPart;
            String signature = encode(sign(signingInput));
            return signingInput + "." + signature;
        } catch (Exception exception) {
            throw new IllegalStateException("生成登录凭证失败", exception);
        }
    }

    public JwtUser parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("登录凭证格式不正确");
            }

            byte[] expected = sign(parts[0] + "." + parts[1]);
            byte[] actual = DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new IllegalArgumentException("登录凭证签名无效");
            }

            JsonNode header = objectMapper.readTree(DECODER.decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText())) {
                throw new IllegalArgumentException("登录凭证算法不支持");
            }

            JsonNode payload = objectMapper.readTree(DECODER.decode(parts[1]));
            long exp = payload.path("exp").asLong(0);
            if (exp <= Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("登录已过期，请重新登录");
            }

            String subject = payload.path("sub").asText(null);
            String username = payload.path("username").asText(null);
            String role = payload.path("role").asText(null);
            if (subject == null || username == null || role == null) {
                throw new IllegalArgumentException("登录凭证缺少必要信息");
            }
            return new JwtUser(Long.valueOf(subject), username, role);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("登录凭证无效", exception);
        }
    }

    public long expiresInSeconds() {
        return properties.effectiveExpirationSeconds();
    }

    private byte[] sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] bytes) {
        return ENCODER.encodeToString(bytes);
    }

    private SecretKeySpec buildKey(String secret) {
        try {
            byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
            if (raw.length < 32) {
                raw = MessageDigest.getInstance("SHA-256").digest(raw);
            }
            return new SecretKeySpec(raw, "HmacSHA256");
        } catch (Exception exception) {
            throw new IllegalStateException("初始化 JWT 密钥失败", exception);
        }
    }
}
