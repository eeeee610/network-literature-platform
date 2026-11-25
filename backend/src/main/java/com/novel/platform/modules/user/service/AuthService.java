package com.novel.platform.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.user.dto.AuthResponse;
import com.novel.platform.modules.user.dto.LoginRequest;
import com.novel.platform.modules.user.dto.RegisterRequest;
import com.novel.platform.modules.user.dto.UserProfile;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.entity.UserAccount;
import com.novel.platform.modules.user.mapper.AppUserMapper;
import com.novel.platform.modules.user.mapper.UserAccountMapper;
import com.novel.platform.modules.user.security.JwtService;
import com.novel.platform.modules.user.security.PasswordHasher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AppUserMapper userMapper;
    private final UserAccountMapper accountMapper;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;

    public AuthService(AppUserMapper userMapper, UserAccountMapper accountMapper,
                       PasswordHasher passwordHasher, JwtService jwtService) {
        this.userMapper = userMapper;
        this.accountMapper = accountMapper;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = blankToNull(request.email());
        String phone = blankToNull(request.phone());
        String nickname = blankToNull(request.nickname());
        if (nickname == null) {
            nickname = username;
        }

        if (userMapper.selectCount(Wrappers.<AppUser>lambdaQuery().eq(AppUser::getUsername, username)) > 0) {
            throw new BusinessException(40910, "用户名已存在", HttpStatus.CONFLICT);
        }
        if (email != null && userMapper.selectCount(
                Wrappers.<AppUser>lambdaQuery().eq(AppUser::getEmail, email)) > 0) {
            throw new BusinessException(40911, "邮箱已被使用", HttpStatus.CONFLICT);
        }
        if (phone != null && userMapper.selectCount(
                Wrappers.<AppUser>lambdaQuery().eq(AppUser::getPhone, phone)) > 0) {
            throw new BusinessException(40912, "手机号已被使用", HttpStatus.CONFLICT);
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordHasher.encode(request.password()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setNickname(nickname);
        user.setRole("READER");
        user.setStatus("ACTIVE");
        userMapper.insert(user);

        UserAccount account = new UserAccount();
        account.setUserId(user.getUserId());
        account.setBalance(BigDecimal.ZERO);
        accountMapper.insert(account);

        return issue(user);
    }

    public AuthResponse login(LoginRequest request) {
        String accountName = request.account().trim();
        AppUser user = userMapper.findByAccount(accountName);
        if (user == null || !passwordHasher.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(40101, "账号或密码错误", HttpStatus.UNAUTHORIZED);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(40301, "账号已被锁定或禁用", HttpStatus.FORBIDDEN);
        }
        userMapper.updateLastLogin(user.getUserId(), LocalDateTime.now());
        return issue(user);
    }

    public UserProfile me(Long userId) {
        AppUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40100, "用户不存在或登录已失效", HttpStatus.UNAUTHORIZED);
        }
        return profile(user);
    }

    private AuthResponse issue(AppUser user) {
        String token = jwtService.createToken(user.getUserId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, "Bearer", jwtService.expiresInSeconds(), profile(user));
    }

    private UserProfile profile(AppUser user) {
        return new UserProfile(
                String.valueOf(user.getUserId()),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus()
        );
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
