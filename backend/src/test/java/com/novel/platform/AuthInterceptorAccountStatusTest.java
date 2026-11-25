package com.novel.platform;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.security.AuthInterceptor;
import com.novel.platform.common.security.AuthUser;
import com.novel.platform.common.security.JwtTokenProvider;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.mapper.AppUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorAccountStatusTest {

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

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldAcceptActiveUserAndCreateContext() {
        authenticatedRequest("/api/user/account");
        when(tokenProvider.parseToken("valid-token")).thenReturn(authUser());
        when(userMapper.selectById(101L)).thenReturn(user("ACTIVE"));

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(101L, UserContext.currentUserId());
    }

    @Test
    void shouldRejectLockedUserWithExistingToken() {
        authenticatedRequest("/api/user/account");
        when(tokenProvider.parseToken("valid-token")).thenReturn(authUser());
        when(userMapper.selectById(101L)).thenReturn(user("LOCKED"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, new Object()));

        assertEquals(40301, exception.getCode());
        assertNull(UserContext.get());
    }

    @Test
    void shouldRejectDisabledUserEvenOnPublicNovelRoute() {
        authenticatedRequest("/api/novel/list");
        when(request.getMethod()).thenReturn("GET");
        when(tokenProvider.parseToken("valid-token")).thenReturn(authUser());
        when(userMapper.selectById(101L)).thenReturn(user("DISABLED"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, new Object()));

        assertEquals(40301, exception.getCode());
        assertNull(UserContext.get());
    }

    @Test
    void shouldTreatDeletedAccountAsUnauthenticated() {
        authenticatedRequest("/api/user/account");
        when(tokenProvider.parseToken("valid-token")).thenReturn(authUser());
        when(userMapper.selectById(101L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, new Object()));

        assertEquals(40100, exception.getCode());
        assertNull(UserContext.get());
    }

    private void authenticatedRequest(String path) {
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
    }

    private AuthUser authUser() {
        return new AuthUser(101L, "reader", "READER");
    }

    private AppUser user(String status) {
        AppUser user = new AppUser();
        user.setUserId(101L);
        user.setStatus(status);
        return user;
    }
}
