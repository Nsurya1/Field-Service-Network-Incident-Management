package com.sochi.fieldservice.incident.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_history")
public class IncidentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "performed_by", length = 50)
    private String performedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public IncidentHistory() {
    }

    public IncidentHistory(Incident incident, String action, String description) {
        this(incident, action, description, "system");
    }

    public IncidentHistory(Incident incident, String action, String description, String performedBy) {
        this.incident = incident;
        this.action = action;
        this.description = description;
        this.performedBy = performedBy != null && !performedBy.isBlank() ? performedBy : "system";
    }

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
}
