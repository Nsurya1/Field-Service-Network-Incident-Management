package com.sochi.fieldservice.incident.repository;

import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    Page<Incident> findByPriority(IncidentPriority priority, Pageable pageable);

    Page<Incident> findByStatusAndPriority(
            IncidentStatus status,
            IncidentPriority priority,
            Pageable pageable
    );

    long countByAssignedEngineerIdAndStatusIn(Long engineerId, List<IncidentStatus> statuses);

    long countByStatus(IncidentStatus status);

    long countByStatusInAndSlaDeadlineBefore(List<IncidentStatus> statuses, java.time.LocalDateTime dateTime);
}
