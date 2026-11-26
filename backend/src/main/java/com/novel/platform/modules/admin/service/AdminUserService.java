package com.novel.platform.modules.admin.service;

import com.novel.platform.common.api.PageResult;
import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.admin.dto.AdminUserItem;
import com.novel.platform.modules.admin.dto.AdminUserRow;
import com.novel.platform.modules.admin.dto.UserStatusRequest;
import com.novel.platform.modules.admin.mapper.AdminUserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminUserService {

    private static final Set<String> ROLES = Set.of("READER", "AUTHOR", "ADMIN");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "LOCKED", "DISABLED");

    private final AdminUserMapper userMapper;

    public AdminUserService(AdminUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public PageResult<AdminUserItem> list(String keyword, String role, String status,
                                          int pageParam, int pageSizeParam) {
        int page = AdminPagination.page(pageParam);
        int pageSize = AdminPagination.size(pageSizeParam);
        String normalizedKeyword = trimToNull(keyword);
        String normalizedRole = normalizeChoice(role, ROLES, "role");
        String normalizedStatus = normalizeChoice(status, STATUSES, "status");
        List<AdminUserItem> items = userMapper.selectUsers(
                        normalizedKeyword, normalizedRole, normalizedStatus,
                        AdminPagination.offset(page, pageSize), pageSize)
                .stream().map(this::toItem).toList();
        long total = userMapper.countUsers(normalizedKeyword, normalizedRole, normalizedStatus);
        return new PageResult<>(items, total, page, pageSize);
    }

    @Transactional
    public void changeStatus(Long userId, UserStatusRequest request, Long currentAdminId) {
        AdminUserRow user = userMapper.selectUserById(userId);
        if (user == null) {
            throw new BusinessException(40401, "用户不存在", HttpStatus.NOT_FOUND);
        }
        String status = request.status().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw new BusinessException(40000, "status 只能为 ACTIVE、LOCKED 或 DISABLED");
        }
        if (userId.equals(currentAdminId) && !"ACTIVE".equals(status)) {
            throw new BusinessException(40001, "不能锁定或停用当前登录的管理员账号");
        }
        userMapper.updateStatus(userId, status);
    }

    private AdminUserItem toItem(AdminUserRow row) {
        return new AdminUserItem(
                String.valueOf(row.getUserId()), row.getUsername(), row.getNickname(), row.getAvatarUrl(),
                row.getEmail(), row.getPhone(), row.getRole(), row.getStatus(),
                row.getLastLoginAt(), row.getCreatedAt());
    }

    private String normalizeChoice(String value, Set<String> choices, String field) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!choices.contains(normalized)) {
            throw new BusinessException(40000, field + " 参数不正确");
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
