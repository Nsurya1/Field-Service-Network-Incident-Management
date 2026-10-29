package com.sochi.fieldservice.report.repository;

import com.sochi.fieldservice.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Incident, Long> {

    @Query(value = """
        SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (i.resolved_at - i.created_at)) / 3600.0), 0.0)
        FROM incident i
        WHERE i.resolved_at IS NOT NULL
          AND i.created_at >= :from AND i.created_at <= :to
        """, nativeQuery = true)
    Double calculateMttrHours(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT
            COUNT(i.id) AS totalResolved,
            COALESCE(SUM(CASE WHEN i.resolved_at <= i.sla_deadline THEN 1 ELSE 0 END), 0) AS compliantCount
        FROM incident i
        WHERE i.resolved_at IS NOT NULL
          AND i.created_at >= :from AND i.created_at <= :to
        """, nativeQuery = true)
    SlaComplianceProjection calculateSlaCompliance(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT COUNT(i.id)
        FROM incident i
        WHERE i.created_at >= :from AND i.created_at <= :to
        """, nativeQuery = true)
    Long countCreatedIncidents(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT
            i.priority AS priority,
            COUNT(i.id) AS count
        FROM incident i
        WHERE i.created_at >= :from AND i.created_at <= :to
        GROUP BY i.priority
        """, nativeQuery = true)
    List<PriorityCountProjection> countIncidentsByPriority(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT
            e.id AS engineerId,
            e.name AS engineerName,
            COUNT(i.id) AS resolvedCount,
            COALESCE(AVG(EXTRACT(EPOCH FROM (i.resolved_at - i.created_at)) / 3600.0), 0.0) AS avgResolutionTimeHours
        FROM incident i
        JOIN engineers e ON i.assigned_engineer_id = e.id
        WHERE i.resolved_at IS NOT NULL
          AND i.created_at >= :from AND i.created_at <= :to
        GROUP BY e.id, e.name
        ORDER BY resolvedCount DESC, avgResolutionTimeHours ASC
        """, nativeQuery = true)
    List<EngineerPerformanceProjection> calculateEngineerPerformance(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT
            CAST(i.created_at AS DATE) AS day,
            COUNT(i.id) AS count
        FROM incident i
        WHERE i.created_at >= :from AND i.created_at <= :to
        GROUP BY CAST(i.created_at AS DATE)
        ORDER BY day ASC
        """, nativeQuery = true)
    List<DailyCountProjection> countDailyCreated(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT
            CAST(i.resolved_at AS DATE) AS day,
            COUNT(i.id) AS count
        FROM incident i
        WHERE i.resolved_at IS NOT NULL
          AND i.resolved_at >= :from AND i.resolved_at <= :to
        GROUP BY CAST(i.resolved_at AS DATE)
        ORDER BY day ASC
        """, nativeQuery = true)
    List<DailyCountProjection> countDailyResolved(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        SELECT i
        FROM Incident i
        LEFT JOIN FETCH i.assignedEngineer
        WHERE i.createdAt >= :from AND i.createdAt <= :to
        ORDER BY i.createdAt DESC
        """)
    List<Incident> findIncidentsForExport(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
