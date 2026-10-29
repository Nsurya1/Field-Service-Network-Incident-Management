package com.sochi.fieldservice.visit.service;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.service.EngineerService;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.incident.repository.IncidentHistoryRepository;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import com.sochi.fieldservice.visit.dto.CreateServiceVisitRequest;
import com.sochi.fieldservice.visit.dto.ServiceVisitResponse;
import com.sochi.fieldservice.visit.entity.ServiceVisit;
import com.sochi.fieldservice.visit.repository.ServiceVisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class ServiceVisitService {

    private final ServiceVisitRepository serviceVisitRepository;
    private final IncidentRepository incidentRepository;
    private final EngineerService engineerService;
    private final IncidentHistoryRepository incidentHistoryRepository;
    private final UserRepository userRepository;

    public ServiceVisitService(
            ServiceVisitRepository serviceVisitRepository,
            IncidentRepository incidentRepository,
            EngineerService engineerService,
            IncidentHistoryRepository incidentHistoryRepository,
            UserRepository userRepository) {
        this.serviceVisitRepository = serviceVisitRepository;
        this.incidentRepository = incidentRepository;
        this.engineerService = engineerService;
        this.incidentHistoryRepository = incidentHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ServiceVisitResponse createVisit(Long incidentId, CreateServiceVisitRequest request, String username) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Incident not found with ID: " + incidentId
                ));

        Engineer engineer = resolveEngineer(incident, request, username);

        ServiceVisit visit = new ServiceVisit();
        visit.setIncident(incident);
        visit.setEngineer(engineer);
        visit.setVisitDate(request.getVisitDate() != null ? request.getVisitDate() : LocalDate.now());
        visit.setCheckInAt(request.getCheckInAt());
        visit.setCheckOutAt(request.getCheckOutAt());
        visit.setWorkNotes(request.getWorkNotes().trim());
        if (request.getResolutionNotes() != null && !request.getResolutionNotes().isBlank()) {
            visit.setResolutionNotes(request.getResolutionNotes().trim());
        }

        ServiceVisit saved = serviceVisitRepository.save(visit);

        incidentHistoryRepository.save(new IncidentHistory(
                incident,
                "SERVICE_VISIT",
                "Service visit recorded by " + engineer.getName() + ": " + visit.getWorkNotes()
        ));

        return new ServiceVisitResponse(saved);
    }

    public List<ServiceVisitResponse> getVisitsForIncident(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Incident not found with ID: " + incidentId
            );
        }

        return serviceVisitRepository
                .findByIncidentIdOrderByVisitDateAscCheckInAtAscCreatedAtAsc(incidentId)
                .stream()
                .map(ServiceVisitResponse::new)
                .toList();
    }

    private Engineer resolveEngineer(Incident incident, CreateServiceVisitRequest request, String username) {
        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && user.getRole() == Role.ROLE_ENGINEER) {
                if (user.getEngineer() == null
                        || incident.getAssignedEngineer() == null
                        || !incident.getAssignedEngineer().getId().equals(user.getEngineer().getId())) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Engineers can only log service visits on incidents assigned to them"
                    );
                }
                return user.getEngineer();
            }
        }

        Engineer engineer = null;
        if (request.getEngineerId() != null) {
            engineer = engineerService.getEngineerById(request.getEngineerId());
        } else if (incident.getAssignedEngineer() != null) {
            engineer = incident.getAssignedEngineer();
        }

        if (engineer == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "An engineer must be specified or assigned to record a service visit"
            );
        }

        return engineer;
    }
}
