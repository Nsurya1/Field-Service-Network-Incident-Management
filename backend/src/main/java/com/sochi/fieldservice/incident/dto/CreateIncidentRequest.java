package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.incident.entity.IncidentPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateIncidentRequest {

    @NotBlank(message  = "Incident title is required")
    @Size(max = 150,message = "Title cannot exceed 150 characters")
    private String title;

    @Size(max = 5000,message="Description cannot exceed 5000 characters")
    private String description;

    @NotNull(message = "Incident priority is required")
    private IncidentPriority priority;

    @Size(max = 255,message = "Location cannot exceed 255 characters")
    private String location;

    public CreateIncidentRequest(){};

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public void setPriority(IncidentPriority priority) {
        this.priority = priority;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
