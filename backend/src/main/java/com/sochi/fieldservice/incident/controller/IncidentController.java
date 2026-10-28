package com.sochi.fieldservice.incident.controller;

import com.sochi.fieldservice.incident.dto.AssignIncidentRequest;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.dto.IncidentHistoryResponse;
import com.sochi.fieldservice.incident.dto.IncidentResponse;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
import com.sochi.fieldservice.incident.dto.UpdateIncidentStatusRequest;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.IncidentService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
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
            @RequestParam(defaultValue = "10") int size) {

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

        return incidentService
                .getIncidents(status, priority, pageable)
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
            @Valid @RequestBody UpdateIncidentStatusRequest request) {

        return new IncidentResponse(
                incidentService.updateIncidentStatus(
                        id, request.getStatus()));
    }

    @PatchMapping("/{id}/assign")
    public IncidentResponse assignIncident(
            @PathVariable Long id,
            @Valid @RequestBody AssignIncidentRequest request) {

        return new IncidentResponse(
                incidentService.assignIncident(
                        id, request.getEngineerId()));
    }
}
