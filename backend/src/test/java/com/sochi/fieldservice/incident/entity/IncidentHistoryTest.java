package com.sochi.fieldservice.incident.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class IncidentHistoryTest {

    @Test
    void shouldCreateIncidentHistoryWithConstructorAndPrePersist() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");

        IncidentHistory history = new IncidentHistory(incident, "CREATED", "Incident created");
        assertNull(history.getTimestamp());

        history.onCreate();

        assertNotNull(history.getTimestamp());
        assertEquals("CREATED", history.getAction());
        assertEquals("Incident created", history.getDescription());
        assertEquals(incident, history.getIncident());
        assertEquals("system", history.getPerformedBy());
    }

    @Test
    void shouldCreateIncidentHistoryWithPerformedByConstructor() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");

        IncidentHistory history = new IncidentHistory(incident, "ASSIGNED", "Assigned to Bob", "dispatcher");
        assertEquals("dispatcher", history.getPerformedBy());

        IncidentHistory fallback = new IncidentHistory(incident, "ASSIGNED", "Assigned to Bob", "   ");
        assertEquals("system", fallback.getPerformedBy());
    }

    @Test
    void shouldSupportSettersAndGetters() {
        IncidentHistory history = new IncidentHistory();
        history.setAction("ASSIGNED");
        history.setDescription("Assigned to Bob");
        history.setPerformedBy("admin_user");
        LocalDateTime now = LocalDateTime.now();
        history.setTimestamp(now);

        assertEquals("ASSIGNED", history.getAction());
        assertEquals("Assigned to Bob", history.getDescription());
        assertEquals("admin_user", history.getPerformedBy());
        assertEquals(now, history.getTimestamp());
    }

    @Test
    void shouldNotOverwriteExistingTimestampOnPrePersist() {
        IncidentHistory history = new IncidentHistory();
        LocalDateTime customTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        history.setTimestamp(customTime);

        history.onCreate();

        assertEquals(customTime, history.getTimestamp());
    }

    @Test
    void shouldSupportSetIncident() {
        Incident incident = new Incident();
        IncidentHistory history = new IncidentHistory();
        history.setIncident(incident);

        assertEquals(incident, history.getIncident());
    }
}
