package com.sochi.fieldservice.engineer.service;

import com.sochi.fieldservice.engineer.dto.CreateEngineerRequest;
import com.sochi.fieldservice.engineer.dto.EngineerResponse;
import com.sochi.fieldservice.engineer.dto.UpdateEngineerRequest;
import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EngineerService {

    private final EngineerRepository engineerRepository;
    private final IncidentRepository incidentRepository;

    public EngineerService(
            EngineerRepository engineerRepository,
            IncidentRepository incidentRepository) {
        this.engineerRepository = engineerRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public EngineerResponse createEngineer(CreateEngineerRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (engineerRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An engineer with this email already exists"
            );
        }

        Engineer engineer = new Engineer();
        engineer.setName(request.getName().trim());
        engineer.setEmail(email);
        engineer.setSpecialization(request.getSpecialization().trim());
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            engineer.setPhone(request.getPhone().trim());
        }
        engineer.setActive(true);
        engineer.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        Engineer saved = engineerRepository.save(engineer);
        return toResponse(saved, 0L);
    }

    public List<EngineerResponse> getAllEngineers() {
        return engineerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EngineerResponse getEngineerResponseById(Long id) {
        Engineer engineer = getEngineerById(id);
        return toResponse(engineer);
    }

    @Transactional
    public EngineerResponse updateEngineer(Long id, UpdateEngineerRequest request) {
        Engineer engineer = getEngineerById(id);

        engineer.setName(request.getName().trim());
        engineer.setSpecialization(request.getSpecialization().trim());
        if (request.getPhone() != null) {
            engineer.setPhone(request.getPhone().trim());
        }
        if (request.getAvailabilityStatus() != null) {
            engineer.setAvailabilityStatus(request.getAvailabilityStatus());
        }

        Engineer saved = engineerRepository.save(engineer);
        return toResponse(saved);
    }

    @Transactional
    public EngineerResponse updateEngineerActiveStatus(Long id, boolean active) {
        Engineer engineer = getEngineerById(id);
        engineer.setActive(active);
        Engineer saved = engineerRepository.save(engineer);
        return toResponse(saved);
    }

    public Engineer getEngineerById(Long id) {
        return engineerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Engineer not found with ID: " + id
                ));
    }

    @Transactional
    public Engineer getEngineerByIdForUpdate(Long id) {
        return engineerRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Engineer not found with ID: " + id
                ));
    }

    private EngineerResponse toResponse(Engineer engineer) {
        long workload = incidentRepository.countByAssignedEngineerIdAndStatusIn(
                engineer.getId(),
                List.of(IncidentStatus.ASSIGNED, IncidentStatus.IN_PROGRESS)
        );
        return toResponse(engineer, workload);
    }

    private EngineerResponse toResponse(Engineer engineer, long workload) {
        return new EngineerResponse(engineer, workload);
    }
}
