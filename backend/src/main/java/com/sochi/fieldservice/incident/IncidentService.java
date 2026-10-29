package com.sochi.fieldservice.incident;

import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.service.EngineerService;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
import com.sochi.fieldservice.incident.dto.UpdateIncidentStatusRequest;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.entity.RootCause;
import com.sochi.fieldservice.incident.repository.IncidentHistoryRepository;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {
    public static final int MAX_ACTIVE_INCIDENTS_PER_ENGINEER = 3;

    private final IncidentRepository incidentRepository;
    private final EngineerService engineerService;
    private final IncidentHistoryRepository incidentHistoryRepository;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(IncidentService.class);

    public IncidentService(
            IncidentRepository incidentRepository,
            EngineerService engineerService,
            IncidentHistoryRepository incidentHistoryRepository,
            UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.engineerService = engineerService;
        this.incidentHistoryRepository = incidentHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
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

    @Transactional
    public Incident assignIncident(Long incidentId, Long engineerId) {

        Incident incident = getIncidentById(incidentId);

        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot assign engineer to a closed incident"
            );
        }

        // Pessimistic write lock to serialize concurrent assignments to the same engineer and protect workload cap
        Engineer engineer = engineerService.getEngineerByIdForUpdate(engineerId);

        if (!engineer.isActive() || engineer.getAvailabilityStatus() == AvailabilityStatus.OFF_DUTY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Cannot assign incident to engineer %s because they are %s",
                            engineer.getName(),
                            !engineer.isActive() ? "inactive" : "off-duty")
            );
        }

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
        if (incident.getStatus() == IncidentStatus.OPEN) {
            incident.setStatus(IncidentStatus.ASSIGNED);
        }

        Incident savedIncident = incidentRepository.save(incident);

        incidentHistoryRepository.save(new IncidentHistory(
                savedIncident,
                "ASSIGNED",
                "Incident assigned to engineer: " + engineer.getName()
        ));

        return savedIncident;
    }

    @Transactional
    public Incident unassignIncident(Long incidentId) {
        Incident incident = getIncidentById(incidentId);

        if (incident.getStatus() == IncidentStatus.CLOSED || incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot unassign engineer from a " + incident.getStatus() + " incident"
            );
        }

        Engineer previousEngineer = incident.getAssignedEngineer();
        incident.setAssignedEngineer(null);

        if (incident.getStatus() == IncidentStatus.ASSIGNED || incident.getStatus() == IncidentStatus.IN_PROGRESS) {
            incident.setStatus(IncidentStatus.OPEN);
        }

        Incident savedIncident = incidentRepository.save(incident);

        incidentHistoryRepository.save(new IncidentHistory(
                savedIncident,
                "UNASSIGNED",
                previousEngineer != null
                        ? "Incident unassigned from engineer: " + previousEngineer.getName()
                        : "Incident unassigned"
        ));

        return savedIncident;
    }

    public Page<Incident> getIncidents(
            IncidentStatus status,
            IncidentPriority priority,
            Pageable pageable) {
        return getIncidents(status, priority, pageable, null);
    }

    public Page<Incident> getIncidents(
            IncidentStatus status,
            IncidentPriority priority,
            Pageable pageable,
            String username) {

        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && user.getRole() == Role.ROLE_ENGINEER) {
                if (user.getEngineer() == null) {
                    return Page.empty(pageable);
                }
                return getIncidentsForEngineer(user.getEngineer().getId(), status, priority, pageable);
            }
        }

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

    public Page<Incident> getMyIncidents(
            IncidentStatus status,
            IncidentPriority priority,
            Pageable pageable,
            String username) {

        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username required to fetch assigned incidents");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with username: " + username
                ));

        if (user.getEngineer() == null) {
            return Page.empty(pageable);
        }

        return getIncidentsForEngineer(user.getEngineer().getId(), status, priority, pageable);
    }

    private Page<Incident> getIncidentsForEngineer(
            Long engineerId,
            IncidentStatus status,
            IncidentPriority priority,
            Pageable pageable) {

        if (status != null && priority != null) {
            return incidentRepository.findByAssignedEngineerIdAndStatusAndPriority(
                    engineerId, status, priority, pageable);
        }

        if (status != null) {
            return incidentRepository.findByAssignedEngineerIdAndStatus(
                    engineerId, status, pageable);
        }

        if (priority != null) {
            return incidentRepository.findByAssignedEngineerIdAndPriority(
                    engineerId, priority, pageable);
        }

        return incidentRepository.findByAssignedEngineerId(engineerId, pageable);
    }

    @Transactional
    public Incident updateIncidentStatus(
            Long id,
            IncidentStatus newStatus) {
        return updateIncidentStatus(id, newStatus, null, null, null);
    }

    @Transactional
    public Incident updateIncidentStatus(
            Long id,
            IncidentStatus newStatus,
            String resolutionNotes,
            RootCause rootCause) {
        return updateIncidentStatus(id, newStatus, resolutionNotes, rootCause, null);
    }

    @Transactional
    public Incident updateIncidentStatus(
            Long id,
            IncidentStatus newStatus,
            String username) {
        return updateIncidentStatus(id, newStatus, null, null, username);
    }

    @Transactional
    public Incident updateIncidentStatus(
            Long id,
            UpdateIncidentStatusRequest request,
            String username) {
        return updateIncidentStatus(
                id,
                request.getStatus(),
                request.getResolutionNotes(),
                request.getRootCause(),
                username
        );
    }

    @Transactional
    public Incident updateIncidentStatus(
            Long id,
            IncidentStatus newStatus,
            String resolutionNotes,
            RootCause rootCause,
            String username) {

        Incident incident = getIncidentById(id);

        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && user.getRole() == Role.ROLE_ENGINEER) {
                if (incident.getAssignedEngineer() == null ||
                        user.getEngineer() == null ||
                        !incident.getAssignedEngineer().getId().equals(user.getEngineer().getId())) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Engineers can only update status on incidents assigned to them"
                    );
                }
            }
        }

        if (!incident.canTransitionTo(newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Invalid status transition from %s to %s",
                            incident.getStatus(), newStatus)
            );
        }

        if ((newStatus == IncidentStatus.IN_PROGRESS || newStatus == IncidentStatus.RESOLVED)
                && incident.getAssignedEngineer() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot transition to " + newStatus + " without an assigned engineer"
            );
        }

        if (newStatus == IncidentStatus.RESOLVED) {
            if (resolutionNotes == null || resolutionNotes.trim().length() < 10) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Resolution notes of at least 10 characters are required when resolving an incident"
                );
            }
            incident.setResolutionNotes(resolutionNotes.trim());
            if (rootCause != null) {
                incident.setRootCause(rootCause);
            }
        }

        IncidentStatus oldStatus = incident.getStatus();
        incident.setStatus(newStatus);

        Incident savedIncident = incidentRepository.save(incident);

        String description = String.format("Status changed from %s to %s", oldStatus, newStatus);
        if (newStatus == IncidentStatus.RESOLVED) {
            description += ". Resolution: " + resolutionNotes.trim();
            if (rootCause != null) {
                description += " [Root Cause: " + rootCause + "]";
            }
        }

        incidentHistoryRepository.save(new IncidentHistory(
                savedIncident,
                "STATUS_CHANGED",
                description
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
