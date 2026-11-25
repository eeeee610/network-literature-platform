package com.novel.platform.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.platform.modules.user.entity.UserAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 余额变更前锁定账户行，保证同一账户的扣款串行执行。
     */
    @Select("SELECT user_id, balance, created_at, updated_at FROM user_account WHERE user_id = #{userId} FOR UPDATE")
    UserAccount selectForUpdate(@Param("userId") Long userId);
}
