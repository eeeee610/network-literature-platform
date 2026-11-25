package com.novel.platform.modules.admin.controller;

import com.novel.platform.common.api.ApiResponse;
import com.novel.platform.modules.admin.dto.DashboardSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        List<DashboardSummaryResponse.Metric> metrics = List.of(
                new DashboardSummaryResponse.Metric("pendingAudits", "待审核内容", 12, "项"),
                new DashboardSummaryResponse.Metric("userCount", "注册用户", 1280, "人"),
                new DashboardSummaryResponse.Metric("novelCount", "在库作品", 86, "部"),
                new DashboardSummaryResponse.Metric("todayInteractions", "今日互动", 342, "次")
        );
        List<DashboardSummaryResponse.AuditItem> audits = List.of(
                new DashboardSummaryResponse.AuditItem("1001", "小说", "星河拾荒者", "青崖", "2026-11-23 09:40"),
                new DashboardSummaryResponse.AuditItem("1002", "章节", "第十二章：旧城来信", "南山客", "2026-11-23 10:15"),
                new DashboardSummaryResponse.AuditItem("1003", "评论", "这条时间线写得很巧妙", "读者甲", "2026-11-23 10:30")
        );
        return ApiResponse.success(new DashboardSummaryResponse("MOCK", metrics, audits));
    }
}
