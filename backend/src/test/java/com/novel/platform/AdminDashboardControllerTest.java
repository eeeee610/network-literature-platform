package com.novel.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnMockDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.dataSource").value("MOCK"))
                .andExpect(jsonPath("$.data.metrics.length()").value(4))
                .andExpect(jsonPath("$.data.pendingAudits.length()").value(3));
    }

    @Test
    void shouldReturnUnifiedNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("接口不存在"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
