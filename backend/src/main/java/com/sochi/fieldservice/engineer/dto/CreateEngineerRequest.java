package com.sochi.fieldservice.engineer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateEngineerRequest {

    @NotBlank(message = "Engineer name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @NotBlank(message = "Specialization is required")
    @Size(max = 100, message = "Specialization cannot exceed 100 characters")
    private String specialization;

    @Size(max = 30, message = "Phone cannot exceed 30 characters")
    private String phone;

    public CreateEngineerRequest() {
    }

    public CreateEngineerRequest(String name, String email, String specialization) {
        this(name, email, specialization, null);
    }

    public CreateEngineerRequest(String name, String email, String specialization, String phone) {
        this.name = name;
        this.email = email;
        this.specialization = specialization;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
