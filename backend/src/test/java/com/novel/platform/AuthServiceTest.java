package com.novel.platform;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.security.JwtTokenProvider;
import com.novel.platform.modules.user.dto.LoginRequest;
import com.novel.platform.modules.user.dto.LoginResponse;
import com.novel.platform.modules.user.dto.RegisterRequest;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.entity.UserAccount;
import com.novel.platform.modules.user.mapper.AppUserMapper;
import com.novel.platform.modules.user.mapper.UserAccountMapper;
import com.novel.platform.modules.user.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserMapper userMapper;
    @Mock
    private UserAccountMapper accountMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserAndCreateZeroBalanceAccount() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        doAnswer(invocation -> {
            invocation.getArgument(0, AppUser.class).setUserId(101L);
            return 1;
        }).when(userMapper).insert(any(AppUser.class));
        when(tokenProvider.createToken(101L, "reader_101", "READER")).thenReturn("jwt-token");
        when(tokenProvider.getExpireMinutes()).thenReturn(120L);

        LoginResponse response = authService.register(new RegisterRequest(
                "reader_101", "password123", "读者101", "reader@example.com", "13800138000"));

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals("READER", userCaptor.getValue().getRole());
        assertEquals("ACTIVE", userCaptor.getValue().getStatus());
        assertEquals("encoded-password", userCaptor.getValue().getPasswordHash());

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(accountMapper).insert(accountCaptor.capture());
        assertEquals(101L, accountCaptor.getValue().getUserId());
        assertEquals(0, BigDecimal.ZERO.compareTo(accountCaptor.getValue().getBalance()));
        assertEquals("101", response.userId());
        assertEquals("jwt-token", response.token());
    }

    @Test
    void shouldLoginActiveUserAndUpdateLastLoginTime() {
        AppUser user = user(101L, "ACTIVE");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(tokenProvider.createToken(101L, "reader_101", "READER")).thenReturn("jwt-token");
        when(tokenProvider.getExpireMinutes()).thenReturn(120L);

        LoginResponse response = authService.login(new LoginRequest("reader_101", "password123"));

        ArgumentCaptor<AppUser> updateCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userMapper).updateById(updateCaptor.capture());
        assertNotNull(updateCaptor.getValue().getLastLoginAt());
        assertEquals("Bearer", response.tokenType());
        assertEquals(7200L, response.expiresIn());
    }

    @Test
    void shouldRejectWrongPassword() {
        when(userMapper.selectOne(any())).thenReturn(user(101L, "ACTIVE"));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("reader_101", "wrong-password")));

        assertEquals(40101, exception.getCode());
        verify(userMapper, never()).updateById(any(AppUser.class));
    }

    @Test
    void shouldRejectLockedUser() {
        when(userMapper.selectOne(any())).thenReturn(user(101L, "LOCKED"));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("reader_101", "password123")));

        assertEquals(40301, exception.getCode());
        verify(userMapper, never()).updateById(any(AppUser.class));
        verify(tokenProvider, never()).createToken(any(), anyString(), anyString());
    }

    private AppUser user(Long id, String status) {
        AppUser user = new AppUser();
        user.setUserId(id);
        user.setUsername("reader_101");
        user.setPasswordHash("encoded-password");
        user.setNickname("读者101");
        user.setRole("READER");
        user.setStatus(status);
        return user;
    }
}
