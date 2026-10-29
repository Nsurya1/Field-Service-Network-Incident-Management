package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.entity.RootCause;
import jakarta.validation.constraints.NotNull;

public class UpdateIncidentStatusRequest {

    @NotNull(message = "Incident status is required")
    private IncidentStatus status;

    private String resolutionNotes;

    private RootCause rootCause;

    public UpdateIncidentStatusRequest() {
    }

    public UpdateIncidentStatusRequest(IncidentStatus status) {
        this(status, null, null);
    }

    public UpdateIncidentStatusRequest(IncidentStatus status, String resolutionNotes) {
        this(status, resolutionNotes, null);
    }

    public UpdateIncidentStatusRequest(IncidentStatus status, String resolutionNotes, RootCause rootCause) {
        this.status = status;
        this.resolutionNotes = resolutionNotes;
        this.rootCause = rootCause;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public RootCause getRootCause() {
        return rootCause;
    }

    public void setRootCause(RootCause rootCause) {
        this.rootCause = rootCause;
    }
}
