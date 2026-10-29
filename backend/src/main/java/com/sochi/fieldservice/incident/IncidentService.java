package com.sochi.fieldservice.incident;

import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.service.EngineerService;
import com.sochi.fieldservice.incident.dto.CreateIncidentCommentRequest;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.dto.IncidentCommentResponse;
import com.sochi.fieldservice.incident.dto.IncidentFilterCriteria;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
import com.sochi.fieldservice.incident.dto.TimelineItemResponse;
import com.sochi.fieldservice.incident.dto.UpdateIncidentRequest;
import com.sochi.fieldservice.incident.dto.UpdateIncidentStatusRequest;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentComment;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.entity.RootCause;
import com.sochi.fieldservice.incident.repository.IncidentCommentRepository;
import com.sochi.fieldservice.incident.repository.IncidentHistoryRepository;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.incident.specification.IncidentSpecification;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import com.sochi.fieldservice.visit.repository.ServiceVisitRepository;
import com.sochi.fieldservice.observability.BusinessMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sochi.fieldservice.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final IncidentCommentRepository incidentCommentRepository;
    private final ServiceVisitRepository serviceVisitRepository;
    private final BusinessMetricsService businessMetricsService;
    private static final Logger logger = LoggerFactory.getLogger(IncidentService.class);

    public IncidentService(
            IncidentRepository incidentRepository,
            EngineerService engineerService,
            IncidentHistoryRepository incidentHistoryRepository,
            UserRepository userRepository,
            IncidentCommentRepository incidentCommentRepository,
            ServiceVisitRepository serviceVisitRepository,
            BusinessMetricsService businessMetricsService) {
        this.incidentRepository = incidentRepository;
        this.engineerService = engineerService;
        this.incidentHistoryRepository = incidentHistoryRepository;
        this.userRepository = userRepository;
        this.incidentCommentRepository = incidentCommentRepository;
        this.serviceVisitRepository = serviceVisitRepository;
        this.businessMetricsService = businessMetricsService;
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
    public Incident createIncident(CreateIncidentRequest request) {
        return createIncident(request, null);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
    public Incident createIncident(CreateIncidentRequest request, String username) {
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

        if (businessMetricsService != null) {
            businessMetricsService.recordIncidentCreated(savedIncident.getPriority());
        }

        incidentHistoryRepository.save(new IncidentHistory(
                savedIncident,
                "CREATED",
                "Incident created with status OPEN",
                username != null ? username : "system"
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
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_ENGINEER_LIST, CacheConfig.CACHE_AVAILABLE_ENGINEERS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
    public Incident assignIncident(Long incidentId, Long engineerId) {
        return assignIncident(incidentId, engineerId, null);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_ENGINEER_LIST, CacheConfig.CACHE_AVAILABLE_ENGINEERS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
    public Incident assignIncident(Long incidentId, Long engineerId, String username) {
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
                "Incident assigned to engineer: " + engineer.getName(),
                username != null ? username : "system"
        ));

        return savedIncident;
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_ENGINEER_LIST, CacheConfig.CACHE_AVAILABLE_ENGINEERS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
    public Incident unassignIncident(Long incidentId) {
        return unassignIncident(incidentId, null);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_ENGINEER_LIST, CacheConfig.CACHE_AVAILABLE_ENGINEERS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
    public Incident unassignIncident(Long incidentId, String username) {
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
                        : "Incident unassigned",
                username != null ? username : "system"
        ));

        return savedIncident;
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
    public Incident updateIncident(Long id, UpdateIncidentRequest request) {
        return updateIncident(id, request, null);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
    public Incident updateIncident(Long id, UpdateIncidentRequest request, String username) {
        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && user.getRole() != Role.ROLE_ADMIN && user.getRole() != Role.ROLE_MANAGER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators and managers can edit incidents");
            }
        }

        Incident incident = getIncidentById(id);

        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit a closed incident");
        }

        boolean priorityChanged = request.getPriority() != null && request.getPriority() != incident.getPriority();
        IncidentPriority oldPriority = incident.getPriority();

        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setLocation(request.getLocation());

        if (priorityChanged) {
            incident.setPriority(request.getPriority());
            LocalDateTime baseTime = incident.getCreatedAt() != null ? incident.getCreatedAt() : LocalDateTime.now();
            incident.setSlaDeadline(baseTime.plusHours(request.getPriority().getSlaHours()));
        }

        Incident savedIncident = incidentRepository.save(incident);

        String historyDesc = priorityChanged
                ? String.format("Priority changed from %s to %s, SLA deadline recalculated", oldPriority, request.getPriority())
                : "Incident details updated";

        incidentHistoryRepository.save(new IncidentHistory(
                savedIncident,
                "EDITED",
                historyDesc,
                username != null ? username : "system"
        ));

        return savedIncident;
    }

    public Page<Incident> getIncidents(
            IncidentFilterCriteria criteria,
            Pageable pageable,
            String username) {

        IncidentFilterCriteria effectiveCriteria = criteria != null ? criteria : new IncidentFilterCriteria();

        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && user.getRole() == Role.ROLE_ENGINEER) {
                if (user.getEngineer() == null) {
                    return Page.empty(pageable);
                }
                effectiveCriteria.setAssignedEngineerId(user.getEngineer().getId());
            }
        }

        return incidentRepository.findAll(IncidentSpecification.withFilter(effectiveCriteria), pageable);
    }

    public Page<Incident> getIncidents(IncidentFilterCriteria criteria, Pageable pageable) {
        return getIncidents(criteria, pageable, null);
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
        IncidentFilterCriteria criteria = new IncidentFilterCriteria();
        criteria.setStatus(status);
        criteria.setPriority(priority);
        return getIncidents(criteria, pageable, username);
    }

    public Page<Incident> getMyIncidents(
            IncidentFilterCriteria criteria,
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

        IncidentFilterCriteria effectiveCriteria = criteria != null ? criteria : new IncidentFilterCriteria();
        effectiveCriteria.setAssignedEngineerId(user.getEngineer().getId());

        return incidentRepository.findAll(IncidentSpecification.withFilter(effectiveCriteria), pageable);
    }

    public Page<Incident> getMyIncidents(
            IncidentStatus status,
            IncidentPriority priority,
            Pageable pageable,
            String username) {
        IncidentFilterCriteria criteria = new IncidentFilterCriteria();
        criteria.setStatus(status);
        criteria.setPriority(priority);
        return getMyIncidents(criteria, pageable, username);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_ENGINEER_LIST, CacheConfig.CACHE_AVAILABLE_ENGINEERS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
    public Incident updateIncidentStatus(
            Long id,
            IncidentStatus newStatus) {
        return updateIncidentStatus(id, newStatus, null, null, null);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_ENGINEER_LIST, CacheConfig.CACHE_AVAILABLE_ENGINEERS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
    public Incident updateIncidentStatus(
            Long id,
            IncidentStatus newStatus,
            String resolutionNotes,
            RootCause rootCause) {
        return updateIncidentStatus(id, newStatus, resolutionNotes, rootCause, null);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_ENGINEER_LIST, CacheConfig.CACHE_AVAILABLE_ENGINEERS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
    public Incident updateIncidentStatus(
            Long id,
            IncidentStatus newStatus,
            String username) {
        return updateIncidentStatus(id, newStatus, null, null, username);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_ENGINEER_LIST, CacheConfig.CACHE_AVAILABLE_ENGINEERS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
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
    @CacheEvict(value = {CacheConfig.CACHE_INCIDENT_STATS, CacheConfig.CACHE_ENGINEER_LIST, CacheConfig.CACHE_AVAILABLE_ENGINEERS, CacheConfig.CACHE_REPORT_SUMMARY}, allEntries = true)
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

        if (newStatus == IncidentStatus.RESOLVED && oldStatus != IncidentStatus.RESOLVED && businessMetricsService != null) {
            businessMetricsService.recordIncidentResolved(savedIncident.getPriority());
        }

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
                description,
                username != null ? username : "system"
        ));

        return savedIncident;
    }

    @Transactional
    public IncidentCommentResponse addComment(Long incidentId, CreateIncidentCommentRequest request, String username) {
        Incident incident = getIncidentById(incidentId);

        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to add comments");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with username: " + username));

        if (user.getRole() == Role.ROLE_VIEWER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Viewers have read-only access and cannot post comments");
        }

        if (user.getRole() == Role.ROLE_ENGINEER) {
            if (incident.getAssignedEngineer() == null ||
                    user.getEngineer() == null ||
                    !incident.getAssignedEngineer().getId().equals(user.getEngineer().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Engineers can only comment on incidents assigned to them");
            }
        }

        IncidentComment comment = new IncidentComment(incident, username, request.getText().trim());
        IncidentComment savedComment = incidentCommentRepository.save(comment);

        String snippet = request.getText().trim().length() > 50
                ? request.getText().trim().substring(0, 47) + "..."
                : request.getText().trim();

        incidentHistoryRepository.save(new IncidentHistory(
                incident,
                "COMMENT_ADDED",
                "Comment added by " + username + ": " + snippet,
                username
        ));

        return new IncidentCommentResponse(savedComment);
    }

    public List<IncidentCommentResponse> getComments(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found with ID: " + incidentId);
        }
        if (incidentCommentRepository == null) {
            return List.of();
        }
        return incidentCommentRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId)
                .stream()
                .map(IncidentCommentResponse::new)
                .toList();
    }

    public List<TimelineItemResponse> getTimeline(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found with ID: " + incidentId);
        }

        List<TimelineItemResponse> timeline = new java.util.ArrayList<>();

        // Add history events
        incidentHistoryRepository.findByIncidentIdOrderByTimestampAsc(incidentId).stream()
                .map(TimelineItemResponse::fromHistory)
                .forEach(timeline::add);

        // Add comments
        if (incidentCommentRepository != null) {
            incidentCommentRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId).stream()
                    .map(TimelineItemResponse::fromComment)
                    .forEach(timeline::add);
        }

        // Add service visits
        if (serviceVisitRepository != null) {
            serviceVisitRepository.findByIncidentIdOrderByVisitDateAscCheckInAtAscCreatedAtAsc(incidentId).stream()
                    .map(TimelineItemResponse::fromServiceVisit)
                    .forEach(timeline::add);
        }

        java.util.Collections.sort(timeline);
        return timeline;
    }

    public List<IncidentHistory> getIncidentHistory(Long incidentId) {
        getIncidentById(incidentId);
        return incidentHistoryRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
    }

    @Cacheable(value = CacheConfig.CACHE_INCIDENT_STATS, key = "'summary'")
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
