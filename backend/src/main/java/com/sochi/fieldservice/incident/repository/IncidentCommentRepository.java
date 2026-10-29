package com.sochi.fieldservice.incident.repository;

import com.sochi.fieldservice.incident.entity.IncidentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentCommentRepository extends JpaRepository<IncidentComment, Long> {

    List<IncidentComment> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);

    List<IncidentComment> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);

    long countByIncidentId(Long incidentId);
}
