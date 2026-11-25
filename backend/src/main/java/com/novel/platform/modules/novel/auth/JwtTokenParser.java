package com.novel.platform.modules.novel.auth;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.security.AuthUser;
import com.novel.platform.common.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * B 模块作者身份适配器，统一复用 A/公共模块的 JWT 签发与校验配置。
 */
@Component
public class JwtTokenParser {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtTokenParser(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public AuthorPrincipal parse(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw unauthorized();
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw unauthorized();
        }
        try {
            AuthUser user = tokenProvider.parseToken(token);
            return new AuthorPrincipal(user.userId(), user.role());
        } catch (RuntimeException exception) {
            throw unauthorized();
        }
    }

    private BusinessException unauthorized() {
        return new BusinessException(40100, "未登录或登录已失效", HttpStatus.UNAUTHORIZED);
    }
}
