package com.sochi.fieldservice.incident.entity;

import com.sochi.fieldservice.engineer.entity.Engineer;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @Column
    private String location;

    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_engineer_id")
    private Engineer assignedEngineer;

    public Incident() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (slaDeadline == null && priority != null) {
            slaDeadline = now.plusHours(priority.getSlaHours());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
        if (status == IncidentStatus.RESOLVED) {
            if (this.resolvedAt == null) {
                this.resolvedAt = LocalDateTime.now();
            }
        } else if (status == IncidentStatus.CLOSED) {
            if (this.resolvedAt == null) {
                this.resolvedAt = LocalDateTime.now();
            }
            if (this.closedAt == null) {
                this.closedAt = LocalDateTime.now();
            }
        } else if (status == IncidentStatus.IN_PROGRESS || status == IncidentStatus.OPEN || status == IncidentStatus.ASSIGNED) {
            this.resolvedAt = null;
            this.closedAt = null;
        }
    }

    public boolean canTransitionTo(IncidentStatus target) {
        if (this.status == null || target == null || this.status == target) {
            return false;
        }

        return switch (this.status) {
            case OPEN -> target == IncidentStatus.ASSIGNED
                    || target == IncidentStatus.IN_PROGRESS
                    || target == IncidentStatus.CLOSED;
            case ASSIGNED -> target == IncidentStatus.IN_PROGRESS
                    || target == IncidentStatus.OPEN
                    || target == IncidentStatus.CLOSED;
            case IN_PROGRESS -> target == IncidentStatus.RESOLVED
                    || target == IncidentStatus.ASSIGNED;
            case RESOLVED -> target == IncidentStatus.CLOSED
                    || target == IncidentStatus.IN_PROGRESS;
            case CLOSED -> false;
        };
    }

    public boolean isSlaBreached() {
        if (slaDeadline == null) {
            return false;
        }
        if (status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED) {
            LocalDateTime effectiveResolution = resolvedAt != null ? resolvedAt : closedAt;
            return effectiveResolution != null && effectiveResolution.isAfter(slaDeadline);
        }
        return LocalDateTime.now().isAfter(slaDeadline);
    }

    public LocalDateTime getSlaDeadline() {
        return slaDeadline;
    }

    public void setSlaDeadline(LocalDateTime slaDeadline) {
        this.slaDeadline = slaDeadline;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public Engineer getAssignedEngineer() {
        return assignedEngineer;
    }

    public void setAssignedEngineer(Engineer assignedEngineer) {
        this.assignedEngineer = assignedEngineer;
    }
}
