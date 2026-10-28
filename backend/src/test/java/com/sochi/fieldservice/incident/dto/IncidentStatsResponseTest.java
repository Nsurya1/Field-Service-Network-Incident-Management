package com.sochi.fieldservice.incident.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncidentStatsResponseTest {

    @Test
    void shouldSupportAllArgsConstructorAndGetters() {
        IncidentStatsResponse stats = new IncidentStatsResponse(
                10L, 3L, 2L, 1L, 2L, 2L, 6L, 1L
        );

        assertEquals(10L, stats.getTotalIncidents());
        assertEquals(3L, stats.getOpenIncidents());
        assertEquals(2L, stats.getAssignedIncidents());
        assertEquals(1L, stats.getInProgressIncidents());
        assertEquals(2L, stats.getResolvedIncidents());
        assertEquals(2L, stats.getClosedIncidents());
        assertEquals(6L, stats.getActiveIncidents());
        assertEquals(1L, stats.getSlaBreachedIncidents());
    }

    @Test
    void shouldSupportSettersAndNoArgsConstructor() {
        IncidentStatsResponse stats = new IncidentStatsResponse();
        stats.setTotalIncidents(15L);
        stats.setOpenIncidents(5L);
        stats.setAssignedIncidents(4L);
        stats.setInProgressIncidents(3L);
        stats.setResolvedIncidents(2L);
        stats.setClosedIncidents(1L);
        stats.setActiveIncidents(12L);
        stats.setSlaBreachedIncidents(2L);

        assertEquals(15L, stats.getTotalIncidents());
        assertEquals(5L, stats.getOpenIncidents());
        assertEquals(4L, stats.getAssignedIncidents());
        assertEquals(3L, stats.getInProgressIncidents());
        assertEquals(2L, stats.getResolvedIncidents());
        assertEquals(1L, stats.getClosedIncidents());
        assertEquals(12L, stats.getActiveIncidents());
        assertEquals(2L, stats.getSlaBreachedIncidents());
    }
}
