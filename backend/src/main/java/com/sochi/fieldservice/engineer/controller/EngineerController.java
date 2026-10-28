package com.sochi.fieldservice.engineer.controller;

import com.sochi.fieldservice.engineer.dto.CreateEngineerRequest;
import com.sochi.fieldservice.engineer.dto.EngineerResponse;
import com.sochi.fieldservice.engineer.dto.UpdateEngineerActiveRequest;
import com.sochi.fieldservice.engineer.dto.UpdateEngineerRequest;
import com.sochi.fieldservice.engineer.service.EngineerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public EngineerResponse createEngineer(
            @Valid @RequestBody CreateEngineerRequest request) {
        return engineerService.createEngineer(request);
    }

    @GetMapping
    public List<EngineerResponse> getAllEngineers() {
        return engineerService.getAllEngineers();
    }

    @GetMapping("/{id}")
    public EngineerResponse getEngineerById(@PathVariable Long id) {
        return engineerService.getEngineerResponseById(id);
    }

    @PutMapping("/{id}")
    public EngineerResponse updateEngineer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEngineerRequest request) {
        return engineerService.updateEngineer(id, request);
    }

    @PatchMapping("/{id}/active")
    public EngineerResponse updateEngineerActiveStatus(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateEngineerActiveRequest request,
            @RequestParam(required = false) Boolean active) {

        Boolean activeFlag = request != null && request.getActive() != null
                ? request.getActive()
                : active;

        if (activeFlag == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Active status is required in request body or query parameter"
            );
        }

        return engineerService.updateEngineerActiveStatus(id, activeFlag);
    }
}
