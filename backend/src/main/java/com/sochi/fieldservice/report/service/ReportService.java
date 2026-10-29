package com.sochi.fieldservice.report.service;

import com.sochi.fieldservice.config.CacheConfig;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.report.dto.DailyIncidentSeriesResponse;
import com.sochi.fieldservice.report.dto.EngineerPerformanceResponse;
import com.sochi.fieldservice.report.dto.ReportSummaryResponse;
import com.sochi.fieldservice.report.repository.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Cacheable(value = CacheConfig.CACHE_REPORT_SUMMARY, key = "{#from != null ? #from.toString() : 'default_from', #to != null ? #to.toString() : 'default_to'}")
    public ReportSummaryResponse getReportSummary(LocalDateTime from, LocalDateTime to) {
        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' date cannot be after 'to' date");
        }

        // 1. MTTR
        Double rawMttr = reportRepository.calculateMttrHours(effectiveFrom, effectiveTo);
        double mttrHours = rawMttr != null ? Math.round(rawMttr * 100.0) / 100.0 : 0.0;

        // 2. SLA Compliance
        SlaComplianceProjection slaProj = reportRepository.calculateSlaCompliance(effectiveFrom, effectiveTo);
        long totalResolved = slaProj != null && slaProj.getTotalResolved() != null ? slaProj.getTotalResolved() : 0L;
        long compliantResolved = slaProj != null && slaProj.getCompliantCount() != null ? slaProj.getCompliantCount() : 0L;
        double slaCompliancePercentage = totalResolved > 0
                ? Math.round(((double) compliantResolved / totalResolved * 100.0) * 100.0) / 100.0
                : 100.0;

        // 3. Total created
        Long rawTotal = reportRepository.countCreatedIncidents(effectiveFrom, effectiveTo);
        long totalCreated = rawTotal != null ? rawTotal : 0L;

        // 4. Incidents by priority
        Map<IncidentPriority, Long> priorityMap = new LinkedHashMap<>();
        for (IncidentPriority p : IncidentPriority.values()) {
            priorityMap.put(p, 0L);
        }
        List<PriorityCountProjection> priorityList = reportRepository.countIncidentsByPriority(effectiveFrom, effectiveTo);
        if (priorityList != null) {
            for (PriorityCountProjection item : priorityList) {
                if (item.getPriority() != null) {
                    try {
                        IncidentPriority p = IncidentPriority.valueOf(item.getPriority());
                        priorityMap.put(p, item.getCount() != null ? item.getCount() : 0L);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }

        // 5. Engineer performance
        List<EngineerPerformanceProjection> engList = reportRepository.calculateEngineerPerformance(effectiveFrom, effectiveTo);
        List<EngineerPerformanceResponse> engineerPerformance = new ArrayList<>();
        if (engList != null) {
            for (EngineerPerformanceProjection ep : engList) {
                double avgTime = ep.getAvgResolutionTimeHours() != null
                        ? Math.round(ep.getAvgResolutionTimeHours() * 100.0) / 100.0
                        : 0.0;
                engineerPerformance.add(new EngineerPerformanceResponse(
                        ep.getEngineerId(),
                        ep.getEngineerName(),
                        ep.getResolvedCount() != null ? ep.getResolvedCount() : 0L,
                        avgTime
                ));
            }
        }

        // 6. Daily series
        Map<LocalDate, Long> createdByDay = new HashMap<>();
        List<DailyCountProjection> createdDays = reportRepository.countDailyCreated(effectiveFrom, effectiveTo);
        if (createdDays != null) {
            for (DailyCountProjection d : createdDays) {
                if (d.getDay() != null) {
                    createdByDay.put(d.getDay(), d.getCount() != null ? d.getCount() : 0L);
                }
            }
        }

        Map<LocalDate, Long> resolvedByDay = new HashMap<>();
        List<DailyCountProjection> resolvedDays = reportRepository.countDailyResolved(effectiveFrom, effectiveTo);
        if (resolvedDays != null) {
            for (DailyCountProjection d : resolvedDays) {
                if (d.getDay() != null) {
                    resolvedByDay.put(d.getDay(), d.getCount() != null ? d.getCount() : 0L);
                }
            }
        }

        List<DailyIncidentSeriesResponse> dailySeries = new ArrayList<>();
        LocalDate cur = effectiveFrom.toLocalDate();
        LocalDate end = effectiveTo.toLocalDate();
        while (!cur.isAfter(end)) {
            dailySeries.add(new DailyIncidentSeriesResponse(
                    cur,
                    createdByDay.getOrDefault(cur, 0L),
                    resolvedByDay.getOrDefault(cur, 0L)
            ));
            cur = cur.plusDays(1);
        }

        return new ReportSummaryResponse(
                mttrHours,
                slaCompliancePercentage,
                totalCreated,
                totalResolved,
                priorityMap,
                engineerPerformance,
                dailySeries
        );
    }

    public byte[] exportIncidentsCsv(LocalDateTime from, LocalDateTime to) {
        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' date cannot be after 'to' date");
        }

        List<Incident> incidents = reportRepository.findIncidentsForExport(effectiveFrom, effectiveTo);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("Incident ID,Title,Priority,Status,Location,Assigned Engineer,Created At,Resolved At,SLA Deadline,SLA Breached,Resolution Time (Hours)");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Incident inc : incidents) {
            String id = inc.getId() != null ? String.valueOf(inc.getId()) : "";
            String title = escapeCsv(inc.getTitle());
            String priority = inc.getPriority() != null ? inc.getPriority().name() : "";
            String status = inc.getStatus() != null ? inc.getStatus().name() : "";
            String location = escapeCsv(inc.getLocation());
            String engineer = inc.getAssignedEngineer() != null ? escapeCsv(inc.getAssignedEngineer().getName()) : "Unassigned";
            String createdAt = inc.getCreatedAt() != null ? inc.getCreatedAt().format(dtf) : "";
            String resolvedAt = inc.getResolvedAt() != null ? inc.getResolvedAt().format(dtf) : "";
            String slaDeadline = inc.getSlaDeadline() != null ? inc.getSlaDeadline().format(dtf) : "";
            String slaBreached = inc.isSlaBreached() ? "Yes" : "No";

            String resolutionHours = "";
            if (inc.getCreatedAt() != null && inc.getResolvedAt() != null) {
                double hours = ChronoUnit.MINUTES.between(inc.getCreatedAt(), inc.getResolvedAt()) / 60.0;
                resolutionHours = String.format(Locale.US, "%.2f", hours);
            }

            pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    id, title, priority, status, location, engineer, createdAt, resolvedAt, slaDeadline, slaBreached, resolutionHours);
        }

        pw.flush();
        return sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
