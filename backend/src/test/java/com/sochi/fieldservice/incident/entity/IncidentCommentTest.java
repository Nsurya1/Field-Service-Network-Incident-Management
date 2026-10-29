package com.sochi.fieldservice.incident.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class IncidentCommentTest {

    @Test
    @DisplayName("Verify IncidentComment entity constructors and getters/setters")
    void shouldConstructAndManageIncidentComment() {
        Incident incident = new Incident();
        incident.setTitle("Power Loss");

        IncidentComment comment = new IncidentComment(incident, "tech1", "Battery inverter replaced");

        assertEquals(incident, comment.getIncident());
        assertEquals("tech1", comment.getAuthor());
        assertEquals("Battery inverter replaced", comment.getText());
        assertNull(comment.getCreatedAt());

        comment.onCreate();
        assertNotNull(comment.getCreatedAt());

        comment.setAuthor("tech2");
        comment.setText("Updated comment text");
        LocalDateTime fixedTime = LocalDateTime.of(2026, 10, 29, 12, 0);
        comment.setCreatedAt(fixedTime);

        assertEquals("tech2", comment.getAuthor());
        assertEquals("Updated comment text", comment.getText());
        assertEquals(fixedTime, comment.getCreatedAt());

        Incident incident2 = new Incident();
        comment.setIncident(incident2);
        assertEquals(incident2, comment.getIncident());
    }

    @Test
    @DisplayName("Verify all-arguments constructor with explicit createdAt")
    void shouldConstructWithCreatedAt() {
        Incident incident = new Incident();
        LocalDateTime time = LocalDateTime.of(2026, 10, 29, 14, 30);
        IncidentComment comment = new IncidentComment(incident, "manager1", "Reviewing SLA escalation", time);

        assertEquals("manager1", comment.getAuthor());
        assertEquals("Reviewing SLA escalation", comment.getText());
        assertEquals(time, comment.getCreatedAt());
    }
}
