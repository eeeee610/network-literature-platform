package com.novel.content.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.novel.content.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 手写 HS256 JWT（不引入第三方 JWT 库）：Base64URL + JSON payload + HMAC-SHA256。
 */
@Component
public class JwtUtil {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String HEADER = ENCODER.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final SecretKeySpec key;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtUtil(@Value("${app.jwt-secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.jwt-secret 长度必须不少于 32 个字符");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String sign(long userId, String role, long ttlSeconds) {
        long exp = System.currentTimeMillis() / 1000 + ttlSeconds;
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("user_id", userId);
        payload.put("role", role);
        payload.put("exp", exp);
        String payloadPart = ENCODER.encodeToString(toJsonBytes(payload));
        String signingInput = HEADER + "." + payloadPart;
        return signingInput + "." + ENCODER.encodeToString(hmac(signingInput));
    }

    public Payload verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw unauthorized();
            }
            byte[] expected = hmac(parts[0] + "." + parts[1]);
            byte[] provided = DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, provided)) {
                throw unauthorized();
            }
            JsonNode payload = objectMapper.readTree(DECODER.decode(parts[1]));
            long exp = payload.path("exp").asLong(0);
            if (exp <= System.currentTimeMillis() / 1000) {
                throw unauthorized();
            }
            long userId = payload.path("user_id").asLong(0);
            String role = payload.path("role").asText("");
            if (userId <= 0 || role.isBlank()) {
                throw unauthorized();
            }
            return new Payload(userId, role);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw unauthorized();
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JWT 签名计算失败", e);
        }
    }

    private byte[] toJsonBytes(ObjectNode payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JWT 载荷序列化失败", e);
        }
    }

    private BizException unauthorized() {
        return BizException.unauthorized("未登录或令牌失效");
    }

    public record Payload(long userId, String role) {
    }
}
