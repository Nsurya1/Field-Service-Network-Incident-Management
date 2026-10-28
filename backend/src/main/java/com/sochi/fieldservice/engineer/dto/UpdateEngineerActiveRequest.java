package com.sochi.fieldservice.engineer.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateEngineerActiveRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    public UpdateEngineerActiveRequest() {
    }

    public UpdateEngineerActiveRequest(Boolean active) {
        this.active = active;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
