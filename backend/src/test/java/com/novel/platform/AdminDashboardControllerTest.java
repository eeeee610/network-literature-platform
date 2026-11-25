package com.novel.platform;

import com.novel.platform.common.security.JwtTokenProvider;
import com.novel.platform.modules.admin.dto.DashboardSummaryResponse;
import com.novel.platform.modules.admin.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private AdminDashboardService dashboardService;

    @Test
    void shouldRequireAdminForDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void shouldReturnDatabaseDashboardSummaryForAdmin() throws Exception {
        when(dashboardService.summary()).thenReturn(new DashboardSummaryResponse(
                "DB", 12, 8, 20, 4, new BigDecimal("66.00"), 2, new BigDecimal("18.00"),
                new DashboardSummaryResponse.PendingAuditCounts(1, 2, 3, 4),
                List.of(new DashboardSummaryResponse.AuditItem(
                        "9", "NOVEL", "测试作品", "作者", LocalDateTime.of(2026, 11, 25, 10, 0)))));

        mockMvc.perform(get("/api/admin/dashboard/summary").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.dataSource").value("DB"))
                .andExpect(jsonPath("$.data.userCount").value(12))
                .andExpect(jsonPath("$.data.subscriptionAmount").value(66.00))
                .andExpect(jsonPath("$.data.pendingCounts.rating").value(4))
                .andExpect(jsonPath("$.data.recentPendingAudits[0].id").value("9"));
    }

    @Test
    void shouldReturnUnifiedNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/not-found").header("Authorization", adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("接口不存在"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private String adminToken() {
        return "Bearer " + tokenProvider.createToken(1L, "admin", "ADMIN");
    }
}
