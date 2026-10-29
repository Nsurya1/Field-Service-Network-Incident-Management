package com.sochi.fieldservice.incident.repository;

import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    long countByAssignedEngineerIdAndStatusIn(Long engineerId, List<IncidentStatus> statuses);

    long countByStatus(IncidentStatus status);

    long countByStatusInAndSlaDeadlineBefore(List<IncidentStatus> statuses, LocalDateTime dateTime);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Incident i LEFT JOIN FETCH i.assignedEngineer WHERE i.status IN :statuses")
    List<Incident> findActiveIncidentsWithEngineer(@org.springframework.data.repository.query.Param("statuses") List<IncidentStatus> statuses);
}
