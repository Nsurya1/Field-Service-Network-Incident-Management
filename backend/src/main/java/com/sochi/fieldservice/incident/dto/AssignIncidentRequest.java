
package com.sochi.fieldservice.incident.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AssignIncidentRequest {

    @NotNull(message = "Engineer ID is required")
    @Positive(message = "Engineer ID must be positive")
    private Long engineerId;

    public AssignIncidentRequest() {
    }

    public Long getEngineerId() {
        return engineerId;
    }

    public void setEngineerId(Long engineerId) {
        this.engineerId = engineerId;
    }
}