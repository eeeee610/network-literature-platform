package com.novel.platform.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.security.JwtTokenProvider;
import com.novel.platform.modules.user.dto.LoginRequest;
import com.novel.platform.modules.user.dto.LoginResponse;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.mapper.AppUserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(AppUserMapper appUserMapper, PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserMapper.selectOne(
                Wrappers.<AppUser>lambdaQuery().eq(AppUser::getUsername, request.username().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(40101, "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(40301, "账号已被锁定或停用", HttpStatus.FORBIDDEN);
        }

        String token = tokenProvider.createToken(user.getUserId(), user.getUsername(), user.getRole());
        return new LoginResponse(
                token,
                "Bearer",
                tokenProvider.getExpireMinutes() * 60,
                String.valueOf(user.getUserId()),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole()
        );
    }
}
