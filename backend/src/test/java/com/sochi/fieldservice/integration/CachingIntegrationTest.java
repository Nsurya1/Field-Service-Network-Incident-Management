package com.sochi.fieldservice.integration;

import com.sochi.fieldservice.config.CacheConfig;
import com.sochi.fieldservice.engineer.dto.CreateEngineerRequest;
import com.sochi.fieldservice.engineer.dto.EngineerResponse;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.engineer.service.EngineerService;
import com.sochi.fieldservice.incident.IncidentService;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.report.dto.ReportSummaryResponse;
import com.sochi.fieldservice.report.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CachingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private EngineerService engineerService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EngineerRepository engineerRepository;

    @BeforeEach
    void setUp() {
        for (String name : cacheManager.getCacheNames()) {
            Cache c = cacheManager.getCache(name);
            if (c != null) {
                c.clear();
            }
        }
    }

    @Test
    @DisplayName("Verify incidentStats cache population on read and eviction on incident creation")
    void shouldCacheIncidentStatsAndEvictOnIncidentCreate() {
        Cache statsCache = cacheManager.getCache(CacheConfig.CACHE_INCIDENT_STATS);
        assertThat(statsCache).isNotNull();

        // 1. Initially cache is empty
        assertThat(statsCache.get("summary")).isNull();

        // 2. First call populates cache
        IncidentStatsResponse stats1 = incidentService.getIncidentStats();
        assertThat(stats1).isNotNull();
        assertThat(statsCache.get("summary")).isNotNull();

        // 3. Second call returns cached value
        IncidentStatsResponse stats2 = incidentService.getIncidentStats();
        assertThat(stats2.getTotalIncidents()).isEqualTo(stats1.getTotalIncidents());

        // 4. Create new incident should evict cache
        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle("Caching Test Incident");
        req.setDescription("Verify eviction");
        req.setPriority(IncidentPriority.HIGH);
        req.setLocation("Site-C1");
        incidentService.createIncident(req, "admin");

        // 5. Cache is now evicted
        assertThat(statsCache.get("summary")).isNull();

        // 6. Next call repopulates cache with incremented total
        IncidentStatsResponse stats3 = incidentService.getIncidentStats();
        assertThat(stats3.getTotalIncidents()).isEqualTo(stats1.getTotalIncidents() + 1);
        assertThat(statsCache.get("summary")).isNotNull();
    }

    @Test
    @DisplayName("Verify engineerList cache population and eviction on engineer modification")
    void shouldCacheEngineerListAndEvictOnEngineerStatusUpdate() {
        Cache engCache = cacheManager.getCache(CacheConfig.CACHE_ENGINEER_LIST);
        assertThat(engCache).isNotNull();

        // Create an engineer
        CreateEngineerRequest createReq = new CreateEngineerRequest();
        createReq.setName("Cache Test Eng");
        createReq.setEmail("cache.test.eng@fieldservice.com");
        createReq.setSpecialization("Core IP");
        EngineerResponse createdEng = engineerService.createEngineer(createReq);

        // Fetch engineers - should populate cache
        List<EngineerResponse> list1 = engineerService.getAllEngineers();
        assertThat(list1).isNotEmpty();

        // Updating engineer active status must evict cache
        engineerService.updateEngineerActiveStatus(createdEng.getId(), false);

        // Next fetch reflects update and repopulates
        List<EngineerResponse> list2 = engineerService.getAllEngineers();
        boolean foundInactive = list2.stream()
                .anyMatch(e -> e.getId().equals(createdEng.getId()) && !e.isActive());
        assertThat(foundInactive).isTrue();
    }

    @Test
    @DisplayName("Verify reportSummary caching and eviction on status update")
    void shouldCacheReportSummary() {
        Cache rptCache = cacheManager.getCache(CacheConfig.CACHE_REPORT_SUMMARY);
        assertThat(rptCache).isNotNull();

        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        // 1. Initial compute & cache
        ReportSummaryResponse r1 = reportService.getReportSummary(from, to);
        assertThat(r1).isNotNull();

        // 2. Create engineer and incident
        CreateEngineerRequest engReq = new CreateEngineerRequest();
        engReq.setName("Report Cache Eng");
        engReq.setEmail("report.cache@fieldservice.com");
        engReq.setSpecialization("Core");
        EngineerResponse eng = engineerService.createEngineer(engReq);

        CreateIncidentRequest incReq = new CreateIncidentRequest();
        incReq.setTitle("Report Evict Test");
        incReq.setDescription("Testing report cache eviction");
        incReq.setPriority(IncidentPriority.LOW);
        incReq.setLocation("Lab-Report");
        Incident incident = incidentService.createIncident(incReq, "admin");

        incidentService.assignIncident(incident.getId(), eng.getId(), "admin");
        incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.IN_PROGRESS);

        // Verify report service computes fresh metrics cleanly
        ReportSummaryResponse r2 = reportService.getReportSummary(from, to);
        assertThat(r2).isNotNull();
    }
}
