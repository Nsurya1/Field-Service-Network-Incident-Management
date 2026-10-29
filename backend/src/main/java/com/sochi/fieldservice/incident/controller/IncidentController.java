package com.sochi.fieldservice.incident.controller;

import com.sochi.fieldservice.incident.IncidentService;
import com.sochi.fieldservice.incident.dto.AssignIncidentRequest;
import com.sochi.fieldservice.incident.dto.CreateIncidentCommentRequest;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.dto.IncidentCommentResponse;
import com.sochi.fieldservice.incident.dto.IncidentFilterCriteria;
import com.sochi.fieldservice.incident.dto.IncidentHistoryResponse;
import com.sochi.fieldservice.incident.dto.IncidentResponse;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
import com.sochi.fieldservice.incident.dto.TimelineItemResponse;
import com.sochi.fieldservice.incident.dto.UpdateIncidentRequest;
import com.sochi.fieldservice.incident.dto.UpdateIncidentStatusRequest;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.visit.dto.CreateServiceVisitRequest;
import com.sochi.fieldservice.visit.dto.ServiceVisitResponse;
import com.sochi.fieldservice.visit.service.ServiceVisitService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final ServiceVisitService serviceVisitService;

    public IncidentController(
            IncidentService incidentService,
            ServiceVisitService serviceVisitService) {
        this.incidentService = incidentService;
        this.serviceVisitService = serviceVisitService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse createIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            Principal principal) {

        String username = principal != null ? principal.getName() : null;
        return new IncidentResponse(
                incidentService.createIncident(request, username));
    }

    @GetMapping
    public Page<IncidentResponse> getAllIncidents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @RequestParam(required = false) Long assignedEngineerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) Boolean breachedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Principal principal) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        IncidentFilterCriteria criteria = new IncidentFilterCriteria(
                search,
                status,
                priority,
                assignedEngineerId,
                createdFrom,
                createdTo,
                breachedOnly
        );

        String username = principal != null ? principal.getName() : null;

        Page<Incident> result = incidentService.getIncidents(criteria, pageable, username);
        return result.map(IncidentResponse::new);
    }

    @GetMapping("/mine")
    public Page<IncidentResponse> getMyIncidents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) Boolean breachedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Principal principal) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }

        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        IncidentFilterCriteria criteria = new IncidentFilterCriteria(
                search,
                status,
                priority,
                null,
                createdFrom,
                createdTo,
                breachedOnly
        );

        return incidentService
                .getMyIncidents(criteria, pageable, principal.getName())
                .map(IncidentResponse::new);
    }

    @PutMapping("/{id}")
    public IncidentResponse updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return new IncidentResponse(incidentService.updateIncident(id, request, username));
    }

    @GetMapping("/stats")
    public IncidentStatsResponse getIncidentStats() {
        return incidentService.getIncidentStats();
    }

    @GetMapping("/{id}")
    public IncidentResponse getIncidentById(@PathVariable Long id) {
        return new IncidentResponse(
                incidentService.getIncidentById(id));
    }

    @GetMapping("/{id}/history")
    public List<IncidentHistoryResponse> getIncidentHistory(@PathVariable Long id) {
        return incidentService.getIncidentHistory(id)
                .stream()
                .map(IncidentHistoryResponse::new)
                .toList();
    }

    @PatchMapping("/{id}/status")
    public IncidentResponse updateIncidentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentStatusRequest request,
            Principal principal) {

        String username = principal != null ? principal.getName() : null;
        Incident result = incidentService.updateIncidentStatus(id, request, username);
        return new IncidentResponse(result);
    }

    @PatchMapping("/{id}/assign")
    public IncidentResponse assignIncident(
            @PathVariable Long id,
            @Valid @RequestBody AssignIncidentRequest request,
            Principal principal) {

        String username = principal != null ? principal.getName() : null;
        return new IncidentResponse(
                incidentService.assignIncident(
                        id, request.getEngineerId(), username));
    }

    @PatchMapping("/{id}/unassign")
    public IncidentResponse unassignIncident(
            @PathVariable Long id,
            Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return new IncidentResponse(incidentService.unassignIncident(id, username));
    }

    @PostMapping("/{id}/visits")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceVisitResponse createServiceVisit(
            @PathVariable Long id,
            @Valid @RequestBody CreateServiceVisitRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return serviceVisitService.createVisit(id, request, username);
    }

    @GetMapping("/{id}/visits")
    public List<ServiceVisitResponse> getServiceVisits(@PathVariable Long id) {
        return serviceVisitService.getVisitsForIncident(id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentCommentResponse addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateIncidentCommentRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return incidentService.addComment(id, request, username);
    }

    @GetMapping("/{id}/comments")
    public List<IncidentCommentResponse> getComments(@PathVariable Long id) {
        return incidentService.getComments(id);
    }

    @GetMapping("/{id}/timeline")
    public List<TimelineItemResponse> getTimeline(@PathVariable Long id) {
        return incidentService.getTimeline(id);
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        List<Sort.Order> orders = new ArrayList<>();
        String[] parts = sortParam.split(",");
        if (parts.length == 1) {
            orders.add(new Sort.Order(Sort.Direction.ASC, parts[0].trim()));
        } else if (parts.length == 2 && ("asc".equalsIgnoreCase(parts[1].trim()) || "desc".equalsIgnoreCase(parts[1].trim()))) {
            Sort.Direction direction = "asc".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            orders.add(new Sort.Order(direction, parts[0].trim()));
        } else {
            for (int i = 0; i < parts.length; i += 2) {
                String prop = parts[i].trim();
                Sort.Direction direction = (i + 1 < parts.length && "asc".equalsIgnoreCase(parts[i + 1].trim()))
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
                orders.add(new Sort.Order(direction, prop));
            }
        }
        return orders.isEmpty() ? Sort.by(Sort.Direction.DESC, "createdAt") : Sort.by(orders);
    }
}
