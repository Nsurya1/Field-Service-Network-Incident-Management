package com.sochi.fieldservice.incident;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.service.EngineerService;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.repository.IncidentHistoryRepository;
import com.sochi.fieldservice.incident.repository.IncidentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {
    public static final int MAX_ACTIVE_INCIDENTS_PER_ENGINEER = 3;

    private final IncidentRepository incidentRepository;
    private final EngineerService engineerService;
    private final IncidentHistoryRepository incidentHistoryRepository;
    private static final Logger logger =
            LoggerFactory.getLogger(IncidentService.class);

    public IncidentService(
            IncidentRepository incidentRepository,
            EngineerService engineerService,
            IncidentHistoryRepository incidentHistoryRepository) {
        this.incidentRepository = incidentRepository;
        this.engineerService = engineerService;
        this.incidentHistoryRepository = incidentHistoryRepository;
    }

    public Incident createIncident(CreateIncidentRequest request) {
        Incident incident = new Incident();

        incident.setTitle(request.getTitle().trim());
        incident.setDescription(request.getDescription());
        incident.setPriority(request.getPriority());
        incident.setLocation(request.getLocation());
        incident.setStatus(IncidentStatus.OPEN);

        if (request.getPriority() != null) {
            incident.setSlaDeadline(LocalDateTime.now().plusHours(request.getPriority().getSlaHours()));
        }

        logger.info("Creating incident with title: {}", request.getTitle());

        Incident savedIncident = incidentRepository.save(incident);

        logger.info("Incident created successfully with ID: {}", savedIncident.getId());

        incidentHistoryRepository.save(new IncidentHistory(
                savedIncident,
                "CREATED",
                "Incident created with status OPEN"
        ));

        return savedIncident;
    }

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Incident not found with ID: " + id
                ));
    }

    public Incident assignIncident(Long incidentId, Long engineerId) {

        Incident incident = getIncidentById(incidentId);

        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot assign engineer to a closed incident"
            );
        }

        Engineer engineer = engineerService.getEngineerById(engineerId);

        boolean isAlreadyAssignedToThisEngineer = incident.getAssignedEngineer() != null
                && engineerId.equals(incident.getAssignedEngineer().getId());

        if (!isAlreadyAssignedToThisEngineer) {
            long activeWorkload = incidentRepository.countByAssignedEngineerIdAndStatusIn(
                    engineerId,
                    List.of(IncidentStatus.ASSIGNED, IncidentStatus.IN_PROGRESS)
            );

            if (activeWorkload >= MAX_ACTIVE_INCIDENTS_PER_ENGINEER) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        String.format("Engineer %s has reached maximum active workload capacity (%d incidents)",
                                engineer.getName(), MAX_ACTIVE_INCIDENTS_PER_ENGINEER)
                );
            }
        }

        incident.setAssignedEngineer(engineer);
        incident.setStatus(IncidentStatus.ASSIGNED);

        Incident savedIncident = incidentRepository.save(incident);

        incidentHistoryRepository.save(new IncidentHistory(
                savedIncident,
                "ASSIGNED",
                "Incident assigned to engineer: " + engineer.getName()
        ));

        return savedIncident;
    }

    public Page<Incident> getIncidents(
            IncidentStatus status,
            IncidentPriority priority,
            Pageable pageable) {

        if (status != null && priority != null) {
            return incidentRepository.findByStatusAndPriority(
                    status, priority, pageable);
        }

        if (status != null) {
            return incidentRepository.findByStatus(status, pageable);
        }

        if (priority != null) {
            return incidentRepository.findByPriority(priority, pageable);
        }

        return incidentRepository.findAll(pageable);
    }

    public Incident updateIncidentStatus(
            Long id,
            IncidentStatus newStatus) {

        Incident incident = getIncidentById(id);

        if (!incident.canTransitionTo(newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Invalid status transition from %s to %s",
                            incident.getStatus(), newStatus)
            );
        }

        IncidentStatus oldStatus = incident.getStatus();
        incident.setStatus(newStatus);

        Incident savedIncident = incidentRepository.save(incident);

        incidentHistoryRepository.save(new IncidentHistory(
                savedIncident,
                "STATUS_CHANGED",
                String.format("Status changed from %s to %s", oldStatus, newStatus)
        ));

        return savedIncident;
    }

    public List<IncidentHistory> getIncidentHistory(Long incidentId) {
        getIncidentById(incidentId);
        return incidentHistoryRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
    }

    public IncidentStatsResponse getIncidentStats() {
        long total = incidentRepository.count();
        long open = incidentRepository.countByStatus(IncidentStatus.OPEN);
        long assigned = incidentRepository.countByStatus(IncidentStatus.ASSIGNED);
        long inProgress = incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS);
        long resolved = incidentRepository.countByStatus(IncidentStatus.RESOLVED);
        long closed = incidentRepository.countByStatus(IncidentStatus.CLOSED);
        long active = open + assigned + inProgress;
        long breached = incidentRepository.countByStatusInAndSlaDeadlineBefore(
                List.of(IncidentStatus.OPEN, IncidentStatus.ASSIGNED, IncidentStatus.IN_PROGRESS),
                LocalDateTime.now()
        );

        return new IncidentStatsResponse(
                total, open, assigned, inProgress, resolved, closed, active, breached
        );
    }

}
