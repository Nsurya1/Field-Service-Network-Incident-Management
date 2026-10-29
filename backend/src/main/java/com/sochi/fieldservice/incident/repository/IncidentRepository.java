package com.sochi.fieldservice.incident.repository;

import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    long countByAssignedEngineerIdAndStatusIn(Long engineerId, List<IncidentStatus> statuses);

    long countByStatus(IncidentStatus status);

    long countByStatusInAndSlaDeadlineBefore(List<IncidentStatus> statuses, LocalDateTime dateTime);

    @Query("SELECT i FROM Incident i LEFT JOIN FETCH i.assignedEngineer WHERE i.status IN :statuses")
    List<Incident> findActiveIncidentsWithEngineer(@Param("statuses") List<IncidentStatus> statuses);

    @Query("SELECT i.assignedEngineer.id AS engineerId, COUNT(i.id) AS activeCount " +
           "FROM Incident i " +
           "WHERE i.status IN :statuses AND i.assignedEngineer IS NOT NULL " +
           "GROUP BY i.assignedEngineer.id")
    List<EngineerWorkloadProjection> countActiveIncidentsByEngineer(@Param("statuses") List<IncidentStatus> statuses);

    @Override
    @EntityGraph(attributePaths = {"assignedEngineer"})
    Page<Incident> findAll(Specification<Incident> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"assignedEngineer"})
    Optional<Incident> findById(Long id);
}
