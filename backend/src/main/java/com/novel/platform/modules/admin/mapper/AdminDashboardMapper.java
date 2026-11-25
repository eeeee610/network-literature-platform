package com.novel.platform.modules.admin.mapper;

import com.novel.platform.modules.admin.dto.DashboardPendingAuditRow;
import com.novel.platform.modules.admin.dto.DashboardStatsRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminDashboardMapper {

    DashboardStatsRow selectStats();

    List<DashboardPendingAuditRow> selectRecentPendingAudits();
}
