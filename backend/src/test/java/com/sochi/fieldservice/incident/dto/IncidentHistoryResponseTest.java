package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncidentHistoryResponseTest {

    @Test
    void shouldMapIncidentHistoryToResponse() {
        Incident incident = mock(Incident.class);
        when(incident.getId()).thenReturn(10L);

        IncidentHistory history = new IncidentHistory(incident, "STATUS_CHANGED", "Moved to IN_PROGRESS");
        LocalDateTime now = LocalDateTime.now();
        history.setTimestamp(now);

        IncidentHistoryResponse response = new IncidentHistoryResponse(history);

        assertEquals(10L, response.getIncidentId());
        assertEquals("STATUS_CHANGED", response.getAction());
        assertEquals("Moved to IN_PROGRESS", response.getDescription());
        assertEquals(now, response.getTimestamp());
        assertNull(response.getId());
    }

    @Test
    void shouldHandleNullIncidentGracefully() {
        IncidentHistory history = new IncidentHistory(null, "CREATED", "Created");
        IncidentHistoryResponse response = new IncidentHistoryResponse(history);

        assertNull(response.getIncidentId());
        assertEquals("CREATED", response.getAction());
    }
}
