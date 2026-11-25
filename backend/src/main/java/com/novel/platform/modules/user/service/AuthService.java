package com.novel.platform.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.security.JwtTokenProvider;
import com.novel.platform.modules.user.dto.LoginRequest;
import com.novel.platform.modules.user.dto.LoginResponse;
import com.novel.platform.modules.user.dto.RegisterRequest;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.entity.UserAccount;
import com.novel.platform.modules.user.mapper.AppUserMapper;
import com.novel.platform.modules.user.mapper.UserAccountMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AppUserMapper appUserMapper;
    private final UserAccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(AppUserMapper appUserMapper, UserAccountMapper accountMapper,
                       PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.appUserMapper = appUserMapper;
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = UserSupport.emptyToNull(request.email());
        String phone = UserSupport.emptyToNull(request.phone());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw UserSupport.badRequest("password: UTF-8 字节长度不能超过 72");
        }
        ensureUnique(username, email, phone);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole("READER");
        user.setStatus("ACTIVE");
        try {
            appUserMapper.insert(user);
            UserAccount account = new UserAccount();
            account.setUserId(user.getUserId());
            account.setBalance(java.math.BigDecimal.ZERO);
            accountMapper.insert(account);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(41002, "用户名、邮箱或手机号已被使用", HttpStatus.CONFLICT);
        }
        return loginResult(user);
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
        AppUser update = new AppUser();
        update.setUserId(user.getUserId());
        update.setLastLoginAt(LocalDateTime.now());
        appUserMapper.updateById(update);
        return loginResult(user);
    }

    private LoginResponse loginResult(AppUser user) {
        String token = tokenProvider.createToken(user.getUserId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, "Bearer", tokenProvider.getExpireMinutes() * 60,
                String.valueOf(user.getUserId()), user.getUsername(), user.getNickname(),
                user.getAvatarUrl(), user.getRole());
    }

    private void ensureUnique(String username, String email, String phone) {
        if (appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getUsername, username)) > 0) {
            throw new BusinessException(41002, "用户名已被使用", HttpStatus.CONFLICT);
        }
        if (email != null && appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getEmail, email)) > 0) {
            throw new BusinessException(41002, "邮箱已被使用", HttpStatus.CONFLICT);
        }
        if (phone != null && appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getPhone, phone)) > 0) {
            throw new BusinessException(41002, "手机号已被使用", HttpStatus.CONFLICT);
        }
    }
}
