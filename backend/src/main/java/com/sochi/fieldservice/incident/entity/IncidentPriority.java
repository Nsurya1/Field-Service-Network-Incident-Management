package com.sochi.fieldservice.incident.entity;

public enum IncidentPriority {
    LOW(72),
    MEDIUM(24),
    HIGH(8),
    CRITICAL(4);

    private final int slaHours;

    IncidentPriority(int slaHours) {
        this.slaHours = slaHours;
    }

    public int getSlaHours() {
        return slaHours;
    }
}
