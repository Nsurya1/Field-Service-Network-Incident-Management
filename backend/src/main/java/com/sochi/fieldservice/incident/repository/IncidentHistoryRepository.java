package com.sochi.fieldservice.incident.repository;

import com.sochi.fieldservice.incident.entity.IncidentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {
    List<IncidentHistory> findByIncidentIdOrderByTimestampAsc(Long incidentId);
}
