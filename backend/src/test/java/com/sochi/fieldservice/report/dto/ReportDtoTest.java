package com.sochi.fieldservice.report.dto;

import com.sochi.fieldservice.incident.entity.IncidentPriority;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportDtoTest {

    @Test
    void shouldTestEngineerPerformanceResponse() {
        EngineerPerformanceResponse resp = new EngineerPerformanceResponse(1L, "Alice", 5L, 2.5);
        assertThat(resp.getEngineerId()).isEqualTo(1L);
        assertThat(resp.getEngineerName()).isEqualTo("Alice");
        assertThat(resp.getResolvedCount()).isEqualTo(5L);
        assertThat(resp.getAvgResolutionTimeHours()).isEqualTo(2.5);

        resp.setEngineerId(2L);
        resp.setEngineerName("Bob");
        resp.setResolvedCount(10L);
        resp.setAvgResolutionTimeHours(3.0);

        assertThat(resp.getEngineerId()).isEqualTo(2L);
        assertThat(resp.getEngineerName()).isEqualTo("Bob");
        assertThat(resp.getResolvedCount()).isEqualTo(10L);
        assertThat(resp.getAvgResolutionTimeHours()).isEqualTo(3.0);
    }

    @Test
    void shouldTestDailyIncidentSeriesResponse() {
        LocalDate date = LocalDate.of(2026, 10, 25);
        DailyIncidentSeriesResponse resp = new DailyIncidentSeriesResponse(date, 8L, 6L);
        assertThat(resp.getDate()).isEqualTo(date);
        assertThat(resp.getCreatedCount()).isEqualTo(8L);
        assertThat(resp.getResolvedCount()).isEqualTo(6L);

        LocalDate next = date.plusDays(1);
        resp.setDate(next);
        resp.setCreatedCount(10L);
        resp.setResolvedCount(9L);

        assertThat(resp.getDate()).isEqualTo(next);
        assertThat(resp.getCreatedCount()).isEqualTo(10L);
        assertThat(resp.getResolvedCount()).isEqualTo(9L);
    }

    @Test
    void shouldTestReportSummaryResponse() {
        EngineerPerformanceResponse ep = new EngineerPerformanceResponse(1L, "Alice", 4L, 1.8);
        DailyIncidentSeriesResponse ds = new DailyIncidentSeriesResponse(LocalDate.now(), 5L, 3L);

        ReportSummaryResponse summary = new ReportSummaryResponse(
                2.75,
                92.5,
                20L,
                15L,
                Map.of(IncidentPriority.CRITICAL, 5L, IncidentPriority.HIGH, 15L),
                List.of(ep),
                List.of(ds)
        );

        assertThat(summary.getMttrHours()).isEqualTo(2.75);
        assertThat(summary.getSlaCompliancePercentage()).isEqualTo(92.5);
        assertThat(summary.getTotalIncidents()).isEqualTo(20L);
        assertThat(summary.getResolvedIncidents()).isEqualTo(15L);
        assertThat(summary.getIncidentsByPriority()).containsEntry(IncidentPriority.CRITICAL, 5L);
        assertThat(summary.getEngineerPerformance()).hasSize(1);
        assertThat(summary.getDailyCreatedVsResolved()).hasSize(1);

        summary.setMttrHours(3.1);
        summary.setSlaCompliancePercentage(95.0);
        summary.setTotalIncidents(30L);
        summary.setResolvedIncidents(25L);
        summary.setIncidentsByPriority(Map.of());
        summary.setEngineerPerformance(List.of());
        summary.setDailyCreatedVsResolved(List.of());

        assertThat(summary.getMttrHours()).isEqualTo(3.1);
        assertThat(summary.getSlaCompliancePercentage()).isEqualTo(95.0);
        assertThat(summary.getTotalIncidents()).isEqualTo(30L);
        assertThat(summary.getResolvedIncidents()).isEqualTo(25L);
    }
}
