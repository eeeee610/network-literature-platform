package com.novel.platform.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novel.platform.common.api.PageParam;
import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.auth.CurrentUser;
import com.novel.platform.common.auth.JwtUtil;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.exception.ErrorCodes;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.entity.UserAccount;
import com.novel.platform.modules.user.mapper.AppUserMapper;
import com.novel.platform.modules.user.mapper.UserAccountMapper;
import com.novel.platform.modules.user.model.Requests;
import com.novel.platform.modules.user.model.Views;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AppUserMapper appUserMapper;
    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(AppUserMapper appUserMapper, UserAccountMapper userAccountMapper,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.appUserMapper = appUserMapper;
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(rollbackFor = Exception.class)
    public Views.RegisterResult register(Requests.Register request) {
        String username = request.username().trim();
        Long exists = appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw BusinessException.conflict(ErrorCodes.DUPLICATE, "用户名已被注册");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(isBlank(request.nickname()) ? username : request.nickname().trim());
        user.setEmail(isBlank(request.email()) ? null : request.email().trim());
        user.setPhone(isBlank(request.phone()) ? null : request.phone().trim());
        user.setRole("READER");
        user.setStatus("ACTIVE");
        appUserMapper.insert(user);

        UserAccount account = new UserAccount();
        account.setUserId(user.getUserId());
        account.setBalance(new BigDecimal("0.00"));
        userAccountMapper.insert(account);

        return new Views.RegisterResult(String.valueOf(user.getUserId()), user.getUsername(),
                user.getNickname(), user.getRole());
    }

    public Views.LoginResult login(Requests.Login request) {
        AppUser user = appUserMapper.selectOne(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getUsername, request.username().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCodes.LOGIN_FAILED, HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw BusinessException.forbidden("账号已被锁定或禁用");
        }

        user.setLastLoginAt(LocalDateTime.now());
        appUserMapper.updateById(user);

        String token = jwtUtil.generate(user.getUserId(), user.getUsername(), user.getRole());
        return new Views.LoginResult(token, "Bearer", jwtUtil.expiresInSeconds(), toUserInfo(user));
    }

    public Views.UserInfo me() {
        AppUser user = appUserMapper.selectById(CurrentUser.userId());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw BusinessException.unauthorized("未登录或登录已失效");
        }
        return toUserInfo(user);
    }

    public PageResult<Views.AdminUserItem> adminUsers(Integer page, Integer pageSize, String keyword) {
        PageParam param = PageParam.of(page, pageSize);
        String kw = isBlank(keyword) ? null : keyword.trim();
        Page<AppUser> result = appUserMapper.selectPage(new Page<>(param.page(), param.pageSize()),
                Wrappers.<AppUser>lambdaQuery()
                        .and(kw != null, w -> w.like(AppUser::getUsername, kw).or().like(AppUser::getNickname, kw))
                        .orderByDesc(AppUser::getCreatedAt));
        return PageResult.of(result.getRecords(), result.getTotal(), param.page(), param.pageSize(),
                u -> new Views.AdminUserItem(String.valueOf(u.getUserId()), u.getUsername(), u.getNickname(),
                        u.getEmail(), u.getRole(), u.getStatus(), u.getCreatedAt()));
    }

    private Views.UserInfo toUserInfo(AppUser user) {
        return new Views.UserInfo(String.valueOf(user.getUserId()), user.getUsername(),
                user.getNickname(), user.getAvatarUrl(), user.getRole());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
