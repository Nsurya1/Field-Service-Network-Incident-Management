package com.sochi.fieldservice.report.controller;

import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.report.dto.DailyIncidentSeriesResponse;
import com.sochi.fieldservice.report.dto.EngineerPerformanceResponse;
import com.sochi.fieldservice.report.dto.ReportSummaryResponse;
import com.sochi.fieldservice.report.service.ReportService;
import com.sochi.fieldservice.security.CustomUserDetailsService;
import com.sochi.fieldservice.security.SecurityConfig;
import com.sochi.fieldservice.security.jwt.JwtAccessDeniedHandler;
import com.sochi.fieldservice.security.jwt.JwtAuthenticationEntryPoint;
import com.sochi.fieldservice.security.jwt.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ReportController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @MockitoBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Test
    void shouldGetReportSummary() throws Exception {
        ReportSummaryResponse summary = new ReportSummaryResponse(
                3.2,
                94.5,
                25L,
                20L,
                Map.of(IncidentPriority.CRITICAL, 5L, IncidentPriority.HIGH, 10L),
                List.of(new EngineerPerformanceResponse(1L, "Alice Tech", 10L, 2.5)),
                List.of(new DailyIncidentSeriesResponse(LocalDate.now(), 5L, 4L))
        );

        when(reportService.getReportSummary(any(), any())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/reports/summary")
                        .param("from", "2026-10-01T00:00:00")
                        .param("to", "2026-10-25T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mttrHours").value(3.2))
                .andExpect(jsonPath("$.slaCompliancePercentage").value(94.5))
                .andExpect(jsonPath("$.totalIncidents").value(25))
                .andExpect(jsonPath("$.resolvedIncidents").value(20))
                .andExpect(jsonPath("$.engineerPerformance[0].engineerName").value("Alice Tech"));

        verify(reportService).getReportSummary(any(), any());
    }

    @Test
    void shouldExportCsv() throws Exception {
        byte[] csv = "Incident ID,Title\n1,Fiber Cut".getBytes(StandardCharsets.UTF_8);
        when(reportService.exportIncidentsCsv(any(), any())).thenReturn(csv);

        mockMvc.perform(get("/api/v1/reports/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"incident-report.csv\""))
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(content().string("Incident ID,Title\n1,Fiber Cut"));

        verify(reportService).exportIncidentsCsv(any(), any());
    }
}
