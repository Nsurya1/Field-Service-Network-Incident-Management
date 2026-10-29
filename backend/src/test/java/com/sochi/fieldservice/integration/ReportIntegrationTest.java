package com.sochi.fieldservice.integration;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.report.dto.ReportSummaryResponse;
import com.sochi.fieldservice.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EngineerRepository engineerRepository;

    @Test
    @DisplayName("Verify PostgreSQL database aggregate queries calculate accurate MTTR and SLA compliance")
    void shouldCalculateAccurateMttrAndSlaComplianceInDatabase() {
        Engineer engineer = new Engineer();
        engineer.setName("Rajesh Metrics");
        engineer.setEmail("rajesh.metrics@fieldservice.com");
        engineer.setSpecialization("Optical DWDM");
        Engineer savedEng = engineerRepository.save(engineer);

        LocalDateTime now = LocalDateTime.now();

        // Incident 1: 2 hours resolution, resolved within 4h SLA deadline
        Incident i1 = new Incident();
        i1.setTitle("Optical Line Degradation");
        i1.setDescription("Signal loss on port 1");
        i1.setLocation("Indore Core");
        i1.setPriority(IncidentPriority.CRITICAL);
        i1.setStatus(IncidentStatus.RESOLVED);
        i1.setAssignedEngineer(savedEng);
        i1.setCreatedAt(now.minusHours(10));
        i1.setResolvedAt(now.minusHours(8)); // 2 hours resolution
        i1.setSlaDeadline(now.minusHours(6)); // resolved before deadline
        incidentRepository.save(i1);

        // Incident 2: 6 hours resolution, breached 4h SLA deadline
        Incident i2 = new Incident();
        i2.setTitle("Switch Power Fault");
        i2.setDescription("Secondary PSU failed");
        i2.setLocation("Bhopal Site");
        i2.setPriority(IncidentPriority.CRITICAL);
        i2.setStatus(IncidentStatus.RESOLVED);
        i2.setAssignedEngineer(savedEng);
        i2.setCreatedAt(now.minusHours(12));
        i2.setResolvedAt(now.minusHours(6)); // 6 hours resolution
        i2.setSlaDeadline(now.minusHours(8)); // resolved after deadline (breached)
        incidentRepository.save(i2);

        // Incident 3: Active OPEN incident
        Incident i3 = new Incident();
        i3.setTitle("Pending Ticket");
        i3.setDescription("Unresolved");
        i3.setLocation("Jabalpur");
        i3.setPriority(IncidentPriority.HIGH);
        i3.setStatus(IncidentStatus.OPEN);
        i3.setCreatedAt(now.minusHours(4));
        i3.setSlaDeadline(now.plusHours(4));
        incidentRepository.save(i3);

        LocalDateTime from = now.minusDays(1);
        LocalDateTime to = now.plusDays(1);

        ReportSummaryResponse summary = reportService.getReportSummary(from, to);

        assertThat(summary).isNotNull();
        // MTTR: (2.0 + 6.0) / 2 = 4.0 hours
        assertThat(summary.getMttrHours()).isEqualTo(4.0);
        // SLA Compliance: 1 compliant out of 2 resolved = 50.0%
        assertThat(summary.getSlaCompliancePercentage()).isEqualTo(50.0);
        assertThat(summary.getTotalIncidents()).isGreaterThanOrEqualTo(3L);
        assertThat(summary.getResolvedIncidents()).isGreaterThanOrEqualTo(2L);

        // Engineer performance breakdown
        assertThat(summary.getEngineerPerformance())
                .anyMatch(ep -> ep.getEngineerId().equals(savedEng.getId())
                        && ep.getResolvedCount() == 2L
                        && ep.getAvgResolutionTimeHours() == 4.0);

        // CSV export verification
        byte[] csvBytes = reportService.exportIncidentsCsv(from, to);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("Incident ID,Title,Priority,Status,Location,Assigned Engineer,Created At,Resolved At,SLA Deadline,SLA Breached,Resolution Time (Hours)");
        assertThat(csv).contains("Optical Line Degradation");
        assertThat(csv).contains("Switch Power Fault");
        assertThat(csv).contains("Rajesh Metrics");
    }
}
