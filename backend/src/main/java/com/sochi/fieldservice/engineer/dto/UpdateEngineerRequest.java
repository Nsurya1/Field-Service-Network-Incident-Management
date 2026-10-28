package com.sochi.fieldservice.engineer.dto;

import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateEngineerRequest {

    @NotBlank(message = "Engineer name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Size(max = 30, message = "Phone cannot exceed 30 characters")
    private String phone;

    @NotBlank(message = "Specialization is required")
    @Size(max = 100, message = "Specialization cannot exceed 100 characters")
    private String specialization;

    @NotNull(message = "Availability status is required")
    private AvailabilityStatus availabilityStatus;

    public UpdateEngineerRequest() {
    }

    public UpdateEngineerRequest(String name, String phone, String specialization, AvailabilityStatus availabilityStatus) {
        this.name = name;
        this.phone = phone;
        this.specialization = specialization;
        this.availabilityStatus = availabilityStatus;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
}
