package com.sochi.fieldservice.incident.controller;

import com.sochi.fieldservice.incident.IncidentService;
import com.sochi.fieldservice.incident.dto.AssignIncidentRequest;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.dto.IncidentHistoryResponse;
import com.sochi.fieldservice.incident.dto.IncidentResponse;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
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
            @Valid @RequestBody CreateIncidentRequest request) {

        return new IncidentResponse(
                incidentService.createIncident(request));
    }

    @GetMapping
    public Page<IncidentResponse> getAllIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        String username = principal != null ? principal.getName() : null;

        Page<Incident> result = (username != null)
                ? incidentService.getIncidents(status, priority, pageable, username)
                : incidentService.getIncidents(status, priority, pageable);

        return result.map(IncidentResponse::new);
    }

    @GetMapping("/mine")
    public Page<IncidentResponse> getMyIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
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

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return incidentService
                .getMyIncidents(status, priority, pageable, principal.getName())
                .map(IncidentResponse::new);
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
            @Valid @RequestBody AssignIncidentRequest request) {

        return new IncidentResponse(
                incidentService.assignIncident(
                        id, request.getEngineerId()));
    }

    @PatchMapping("/{id}/unassign")
    public IncidentResponse unassignIncident(@PathVariable Long id) {
        return new IncidentResponse(incidentService.unassignIncident(id));
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
}
