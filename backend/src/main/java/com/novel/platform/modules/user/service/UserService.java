package com.novel.platform.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.common.security.UserContext;
import com.novel.platform.modules.user.dto.UpdateProfileRequest;
import com.novel.platform.modules.user.dto.UserProfileResponse;
import com.novel.platform.modules.user.entity.AppUser;
import com.novel.platform.modules.user.mapper.AppUserMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final AppUserMapper userMapper;

    public UserService(AppUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserProfileResponse profile() {
        return toProfile(requireActiveUser());
    }

    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        AppUser current = requireActiveUser();
        String email = UserSupport.emptyToNull(request.email());
        String phone = UserSupport.emptyToNull(request.phone());
        ensureUnique(email, phone, current.getUserId());

        AppUser update = new AppUser();
        update.setUserId(current.getUserId());
        update.setNickname(request.nickname().trim());
        update.setAvatarUrl(UserSupport.emptyToNull(request.avatarUrl()));
        update.setEmail(email);
        update.setPhone(phone);
        try {
            userMapper.updateById(update);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(41002, "邮箱或手机号已被使用", HttpStatus.CONFLICT);
        }
        return toProfile(requireActiveUser());
    }

    public AppUser requireActiveUser() {
        Long userId = UserContext.currentUserId();
        if (userId == null) {
            throw new BusinessException(40100, "未登录或登录已失效", HttpStatus.UNAUTHORIZED);
        }
        AppUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(41001, "用户不存在", HttpStatus.NOT_FOUND);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(40301, "账号已被锁定或停用", HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private void ensureUnique(String email, String phone, Long excludedUserId) {
        if (email != null && userMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getEmail, email)
                .ne(excludedUserId != null, AppUser::getUserId, excludedUserId)) > 0) {
            throw new BusinessException(41002, "邮箱已被使用", HttpStatus.CONFLICT);
        }
        if (phone != null && userMapper.selectCount(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getPhone, phone)
                .ne(excludedUserId != null, AppUser::getUserId, excludedUserId)) > 0) {
            throw new BusinessException(41002, "手机号已被使用", HttpStatus.CONFLICT);
        }
    }

    private UserProfileResponse toProfile(AppUser user) {
        return new UserProfileResponse(
                String.valueOf(user.getUserId()), user.getUsername(), user.getNickname(),
                user.getAvatarUrl(), user.getEmail(), user.getPhone(), user.getRole(), user.getStatus(),
                UserSupport.iso(user.getLastLoginAt()), UserSupport.iso(user.getCreatedAt()),
                UserSupport.iso(user.getUpdatedAt()));
    }
}
