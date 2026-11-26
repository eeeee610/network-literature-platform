package com.novel.platform.modules.admin.mapper;

import com.novel.platform.modules.admin.dto.AdminUserRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminUserMapper {

    List<AdminUserRow> selectUsers(@Param("keyword") String keyword,
                                   @Param("role") String role,
                                   @Param("status") String status,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    long countUsers(@Param("keyword") String keyword,
                    @Param("role") String role,
                    @Param("status") String status);

    AdminUserRow selectUserById(@Param("userId") Long userId);

    int updateStatus(@Param("userId") Long userId, @Param("status") String status);
}
