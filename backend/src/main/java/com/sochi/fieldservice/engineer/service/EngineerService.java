
package com.sochi.fieldservice.engineer.service;

import com.sochi.fieldservice.engineer.dto.CreateEngineerRequest;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EngineerService {

    private final EngineerRepository engineerRepository;

    public EngineerService(EngineerRepository engineerRepository) {
        this.engineerRepository = engineerRepository;
    }

    public Engineer createEngineer(CreateEngineerRequest request) {

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

        return engineerRepository.save(engineer);
    }

    public List<Engineer> getAllEngineers() {
        return engineerRepository.findAll();
    }

    public Engineer getEngineerById(Long id) {
        return engineerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Engineer not found with ID: " + id
                ));
    }
}
