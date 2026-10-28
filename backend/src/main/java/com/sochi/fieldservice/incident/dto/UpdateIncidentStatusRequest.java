package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.incident.entity.IncidentStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateIncidentStatusRequest {
    @NotNull(message = "Incident status is required")
    private IncidentStatus status;

    public UpdateIncidentStatusRequest() {
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }
}
