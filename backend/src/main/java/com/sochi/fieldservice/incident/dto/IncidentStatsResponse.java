package com.sochi.fieldservice.incident.dto;

public class IncidentStatsResponse {

    private long totalIncidents;
    private long openIncidents;
    private long assignedIncidents;
    private long inProgressIncidents;
    private long resolvedIncidents;
    private long closedIncidents;
    private long activeIncidents;
    private long slaBreachedIncidents;

    public IncidentStatsResponse() {
    }

    public IncidentStatsResponse(
            long totalIncidents,
            long openIncidents,
            long assignedIncidents,
            long inProgressIncidents,
            long resolvedIncidents,
            long closedIncidents,
            long activeIncidents,
            long slaBreachedIncidents) {
        this.totalIncidents = totalIncidents;
        this.openIncidents = openIncidents;
        this.assignedIncidents = assignedIncidents;
        this.inProgressIncidents = inProgressIncidents;
        this.resolvedIncidents = resolvedIncidents;
        this.closedIncidents = closedIncidents;
        this.activeIncidents = activeIncidents;
        this.slaBreachedIncidents = slaBreachedIncidents;
    }

    public long getTotalIncidents() {
        return totalIncidents;
    }

    public void setTotalIncidents(long totalIncidents) {
        this.totalIncidents = totalIncidents;
    }

    public long getOpenIncidents() {
        return openIncidents;
    }

    public void setOpenIncidents(long openIncidents) {
        this.openIncidents = openIncidents;
    }

    public long getAssignedIncidents() {
        return assignedIncidents;
    }

    public void setAssignedIncidents(long assignedIncidents) {
        this.assignedIncidents = assignedIncidents;
    }

    public long getInProgressIncidents() {
        return inProgressIncidents;
    }

    public void setInProgressIncidents(long inProgressIncidents) {
        this.inProgressIncidents = inProgressIncidents;
    }

    public long getResolvedIncidents() {
        return resolvedIncidents;
    }

    public void setResolvedIncidents(long resolvedIncidents) {
        this.resolvedIncidents = resolvedIncidents;
    }

    public long getClosedIncidents() {
        return closedIncidents;
    }

    public void setClosedIncidents(long closedIncidents) {
        this.closedIncidents = closedIncidents;
    }

    public long getActiveIncidents() {
        return activeIncidents;
    }

    public void setActiveIncidents(long activeIncidents) {
        this.activeIncidents = activeIncidents;
    }

    public long getSlaBreachedIncidents() {
        return slaBreachedIncidents;
    }

    public void setSlaBreachedIncidents(long slaBreachedIncidents) {
        this.slaBreachedIncidents = slaBreachedIncidents;
    }
}
