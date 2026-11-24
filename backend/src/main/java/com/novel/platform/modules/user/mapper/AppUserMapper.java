package com.novel.platform.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.platform.modules.user.entity.AppUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface AppUserMapper extends BaseMapper<AppUser> {

    @Select("""
            SELECT *
            FROM app_user
            WHERE username = #{account}
               OR email = #{account}
               OR phone = #{account}
            LIMIT 1
            """)
    AppUser findByAccount(@Param("account") String account);

    @Update("UPDATE app_user SET last_login_at = #{time} WHERE user_id = #{userId}")
    int updateLastLogin(@Param("userId") Long userId, @Param("time") LocalDateTime time);
}
