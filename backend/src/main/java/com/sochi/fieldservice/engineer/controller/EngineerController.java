
package com.sochi.fieldservice.engineer.controller;

import com.sochi.fieldservice.engineer.dto.CreateEngineerRequest;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.service.EngineerService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/engineers")
public class EngineerController {

    private final EngineerService engineerService;

    public EngineerController(EngineerService engineerService) {
        this.engineerService = engineerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Engineer createEngineer(
            @Valid @RequestBody CreateEngineerRequest request) {

        return engineerService.createEngineer(request);
    }

    @GetMapping
    public List<Engineer> getAllEngineers() {
        return engineerService.getAllEngineers();
    }

    @GetMapping("/{id}")
    public Engineer getEngineerById(@PathVariable Long id) {
        return engineerService.getEngineerById(id);
    }
}


