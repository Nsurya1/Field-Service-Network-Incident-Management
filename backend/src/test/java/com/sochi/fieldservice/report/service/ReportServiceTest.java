package com.sochi.fieldservice.report.service;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.report.dto.ReportSummaryResponse;
import com.sochi.fieldservice.report.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void shouldCalculateReportSummarySuccessfully() {
        LocalDateTime from = LocalDateTime.now().minusDays(2);
        LocalDateTime to = LocalDateTime.now();

        when(reportRepository.calculateMttrHours(any(), any())).thenReturn(3.456);

        SlaComplianceProjection slaMock = new SlaComplianceProjection() {
            @Override
            public Long getTotalResolved() { return 10L; }
            @Override
            public Long getCompliantCount() { return 9L; }
        };
        when(reportRepository.calculateSlaCompliance(any(), any())).thenReturn(slaMock);
        when(reportRepository.countCreatedIncidents(any(), any())).thenReturn(15L);

        PriorityCountProjection prioMock = new PriorityCountProjection() {
            @Override
            public String getPriority() { return "CRITICAL"; }
            @Override
            public Long getCount() { return 5L; }
        };
        when(reportRepository.countIncidentsByPriority(any(), any())).thenReturn(List.of(prioMock));

        EngineerPerformanceProjection engMock = new EngineerPerformanceProjection() {
            @Override
            public Long getEngineerId() { return 1L; }
            @Override
            public String getEngineerName() { return "Alice"; }
            @Override
            public Long getResolvedCount() { return 8L; }
            @Override
            public Double getAvgResolutionTimeHours() { return 2.85; }
        };
        when(reportRepository.calculateEngineerPerformance(any(), any())).thenReturn(List.of(engMock));

        DailyCountProjection createdDayMock = new DailyCountProjection() {
            @Override
            public LocalDate getDay() { return LocalDate.now().minusDays(1); }
            @Override
            public Long getCount() { return 4L; }
        };
        when(reportRepository.countDailyCreated(any(), any())).thenReturn(List.of(createdDayMock));

        DailyCountProjection resolvedDayMock = new DailyCountProjection() {
            @Override
            public LocalDate getDay() { return LocalDate.now().minusDays(1); }
            @Override
            public Long getCount() { return 3L; }
        };
        when(reportRepository.countDailyResolved(any(), any())).thenReturn(List.of(resolvedDayMock));

        ReportSummaryResponse result = reportService.getReportSummary(from, to);

        assertThat(result).isNotNull();
        assertThat(result.getMttrHours()).isEqualTo(3.46);
        assertThat(result.getSlaCompliancePercentage()).isEqualTo(90.0);
        assertThat(result.getTotalIncidents()).isEqualTo(15L);
        assertThat(result.getResolvedIncidents()).isEqualTo(10L);
        assertThat(result.getIncidentsByPriority()).containsEntry(IncidentPriority.CRITICAL, 5L);
        assertThat(result.getIncidentsByPriority()).containsEntry(IncidentPriority.HIGH, 0L);
        assertThat(result.getEngineerPerformance()).hasSize(1);
        assertThat(result.getEngineerPerformance().get(0).getEngineerName()).isEqualTo("Alice");
        assertThat(result.getDailyCreatedVsResolved()).isNotEmpty();
    }

    @Test
    void shouldHandleEmptyMetricsGracefully() {
        when(reportRepository.calculateMttrHours(any(), any())).thenReturn(null);
        when(reportRepository.calculateSlaCompliance(any(), any())).thenReturn(null);
        when(reportRepository.countCreatedIncidents(any(), any())).thenReturn(null);
        when(reportRepository.countIncidentsByPriority(any(), any())).thenReturn(List.of());
        when(reportRepository.calculateEngineerPerformance(any(), any())).thenReturn(List.of());
        when(reportRepository.countDailyCreated(any(), any())).thenReturn(List.of());
        when(reportRepository.countDailyResolved(any(), any())).thenReturn(List.of());

        ReportSummaryResponse result = reportService.getReportSummary(null, null);

        assertThat(result).isNotNull();
        assertThat(result.getMttrHours()).isEqualTo(0.0);
        assertThat(result.getSlaCompliancePercentage()).isEqualTo(100.0);
        assertThat(result.getTotalIncidents()).isEqualTo(0L);
        assertThat(result.getResolvedIncidents()).isEqualTo(0L);
    }

    @Test
    void shouldThrowBadRequestWhenFromIsAfterTo() {
        LocalDateTime from = LocalDateTime.now().plusDays(2);
        LocalDateTime to = LocalDateTime.now();

        assertThrows(ResponseStatusException.class, () -> reportService.getReportSummary(from, to));
        assertThrows(ResponseStatusException.class, () -> reportService.exportIncidentsCsv(from, to));
    }

    @Test
    void shouldExportIncidentsToCsv() {
        Incident inc = new Incident();
        ReflectionTestUtils.setField(inc, "id", 101L);
        inc.setTitle("Fiber Cut, Main Street \"A\"");
        inc.setPriority(IncidentPriority.CRITICAL);
        inc.setStatus(IncidentStatus.RESOLVED);
        inc.setLocation("Sector 4\nBasement");
        inc.setCreatedAt(LocalDateTime.of(2026, 10, 20, 10, 0));
        inc.setResolvedAt(LocalDateTime.of(2026, 10, 20, 12, 30));
        inc.setSlaDeadline(LocalDateTime.of(2026, 10, 20, 14, 0));

        Engineer eng = new Engineer();
        eng.setName("Smith, John");
        inc.setAssignedEngineer(eng);

        when(reportRepository.findIncidentsForExport(any(), any())).thenReturn(List.of(inc));

        byte[] csvBytes = reportService.exportIncidentsCsv(null, null);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("Incident ID,Title,Priority,Status,Location,Assigned Engineer,Created At,Resolved At,SLA Deadline,SLA Breached,Resolution Time (Hours)");
        assertThat(csv).contains("101");
        assertThat(csv).contains("\"Fiber Cut, Main Street \"\"A\"\"\"");
        assertThat(csv).contains("CRITICAL");
        assertThat(csv).contains("RESOLVED");
        assertThat(csv).contains("\"Smith, John\"");
        assertThat(csv).contains("2.50");
    }
}
