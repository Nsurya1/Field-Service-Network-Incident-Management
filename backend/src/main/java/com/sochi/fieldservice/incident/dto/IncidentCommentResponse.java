package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.incident.entity.IncidentComment;

import java.time.LocalDateTime;

public class IncidentCommentResponse {

    private Long id;
    private Long incidentId;
    private String author;
    private String text;
    private LocalDateTime createdAt;

    public IncidentCommentResponse() {
    }

    public IncidentCommentResponse(Long id, Long incidentId, String author, String text, LocalDateTime createdAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.author = author;
        this.text = text;
        this.createdAt = createdAt;
    }

    public IncidentCommentResponse(IncidentComment comment) {
        this.id = comment.getId();
        if (comment.getIncident() != null) {
            this.incidentId = comment.getIncident().getId();
        }
        this.author = comment.getAuthor();
        this.text = comment.getText();
        this.createdAt = comment.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public String getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
