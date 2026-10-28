package com.sochi.fieldservice.engineer.dto;

import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import com.sochi.fieldservice.engineer.entity.Engineer;

import java.time.LocalDateTime;

public class EngineerResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String specialization;
    private boolean active;
    private AvailabilityStatus availabilityStatus;
    private long currentWorkload;
    private LocalDateTime createdAt;

    public EngineerResponse(Engineer engineer) {
        this(engineer, 0L);
    }

    public EngineerResponse(Engineer engineer, long currentWorkload) {
        this.id = engineer.getId();
        this.name = engineer.getName();
        this.email = engineer.getEmail();
        this.phone = engineer.getPhone();
        this.specialization = engineer.getSpecialization();
        this.active = engineer.isActive();
        this.availabilityStatus = engineer.getAvailabilityStatus();
        this.currentWorkload = currentWorkload;
        this.createdAt = engineer.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getSpecialization() {
        return specialization;
    }

    public boolean isActive() {
        return active;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public long getCurrentWorkload() {
        return currentWorkload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
