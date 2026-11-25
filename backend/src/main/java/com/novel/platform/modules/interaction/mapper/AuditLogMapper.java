package com.novel.platform.modules.interaction.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 审核日志追加（D 模块 audit_log），与审核状态在同一事务提交。
 */
@Mapper
public interface AuditLogMapper {

    @Insert("INSERT INTO audit_log (admin_id, target_type, target_id, audit_action, audit_reason) "
            + "VALUES (#{adminId}, #{targetType}, #{targetId}, #{auditAction}, #{auditReason})")
    int insert(@Param("adminId") Long adminId,
               @Param("targetType") String targetType,
               @Param("targetId") Long targetId,
               @Param("auditAction") Integer auditAction,
               @Param("auditReason") String auditReason);
}
