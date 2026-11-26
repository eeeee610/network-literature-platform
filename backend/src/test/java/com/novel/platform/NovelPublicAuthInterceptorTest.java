package com.novel.platform;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.security.AuthInterceptor;
import com.novel.platform.common.security.JwtTokenProvider;
import com.novel.platform.modules.user.mapper.AppUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NovelPublicAuthInterceptorTest {

    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private AppUserMapper userMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @InjectMocks
    private AuthInterceptor interceptor;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/novel/list",
            "/api/novel/search",
            "/api/novel/tag/list",
            "/api/novel/chapter/501",
            "/api/novel/301",
            "/api/novel/301/chapters"
    })
    void shouldAllowPublicNovelGetWithoutToken(String path) {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn(path);

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void shouldStillRequireTokenForAuthorArea() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/novel/author/list");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, new Object()));

        assertEquals(40100, exception.getCode());
    }
}
