package com.novel.platform;

import com.novel.platform.common.exception.BusinessException;
import com.novel.platform.modules.admin.dto.AdminUserRow;
import com.novel.platform.modules.admin.dto.UserStatusRequest;
import com.novel.platform.modules.admin.mapper.AdminUserMapper;
import com.novel.platform.modules.admin.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminUserMapper userMapper;

    @InjectMocks
    private AdminUserService userService;

    @Test
    void shouldNotLockCurrentAdmin() {
        when(userMapper.selectUserById(5L)).thenReturn(user(5L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.changeStatus(5L, new UserStatusRequest("LOCKED"), 5L));

        assertEquals(40001, exception.getCode());
        verify(userMapper, never()).updateStatus(5L, "LOCKED");
    }

    @Test
    void shouldChangeAnotherUserStatus() {
        when(userMapper.selectUserById(8L)).thenReturn(user(8L));

        userService.changeStatus(8L, new UserStatusRequest("DISABLED"), 5L);

        verify(userMapper).updateStatus(8L, "DISABLED");
    }

    private AdminUserRow user(Long id) {
        AdminUserRow row = new AdminUserRow();
        row.setUserId(id);
        row.setUsername("user" + id);
        row.setNickname("用户" + id);
        row.setRole("READER");
        row.setStatus("ACTIVE");
        return row;
    }
}
