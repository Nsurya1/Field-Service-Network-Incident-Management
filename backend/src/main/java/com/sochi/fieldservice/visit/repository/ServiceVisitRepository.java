package com.sochi.fieldservice.visit.repository;

import com.sochi.fieldservice.visit.entity.ServiceVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceVisitRepository extends JpaRepository<ServiceVisit, Long> {

    List<ServiceVisit> findByIncidentIdOrderByVisitDateAscCheckInAtAscCreatedAtAsc(Long incidentId);

    List<ServiceVisit> findByEngineerId(Long engineerId);
}
