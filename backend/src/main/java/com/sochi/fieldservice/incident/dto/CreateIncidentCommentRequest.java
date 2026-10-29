package com.sochi.fieldservice.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateIncidentCommentRequest {

    @NotBlank(message = "Comment text cannot be blank")
    @Size(min = 1, max = 2000, message = "Comment text must be between 1 and 2000 characters")
    private String text;

    public CreateIncidentCommentRequest() {
    }

    public CreateIncidentCommentRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
