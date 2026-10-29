package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncidentResponseTest {

    @Test
    void shouldIncludeAssignedEngineerDetailsAndSla() {
        // Arrange
        Engineer engineer = mock(Engineer.class);
        when(engineer.getId()).thenReturn(5L);
        when(engineer.getName()).thenReturn("Rahul Sharma");

        LocalDateTime deadline = LocalDateTime.now().plusHours(4);
        LocalDateTime now = LocalDateTime.now();

        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setDescription("Router is not responding");
        incident.setPriority(IncidentPriority.CRITICAL);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setLocation("Bhopal");
        incident.setAssignedEngineer(engineer);
        incident.setSlaDeadline(deadline);
        incident.setCreatedAt(now);
        incident.setUpdatedAt(now);
        incident.setResolvedAt(now.plusHours(1));
        incident.setClosedAt(now.plusHours(2));
        incident.setResolutionNotes("Resolved by replacing SFP optic");
        incident.setRootCause(com.sochi.fieldservice.incident.entity.RootCause.HARDWARE_FAILURE);

        // Act
        IncidentResponse response = new IncidentResponse(incident);

        // Assert
        assertEquals("Router failure", response.getTitle());
        assertEquals(IncidentStatus.ASSIGNED, response.getStatus());
        assertEquals(5L, response.getAssignedEngineerId());
        assertEquals("Rahul Sharma", response.getAssignedEngineerName());
        assertEquals(deadline, response.getSlaDeadline());
        assertFalse(response.isSlaBreached());
        assertEquals("Resolved by replacing SFP optic", response.getResolutionNotes());
        assertEquals(com.sochi.fieldservice.incident.entity.RootCause.HARDWARE_FAILURE, response.getRootCause());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
        assertEquals(now.plusHours(1), response.getResolvedAt());
        assertEquals(now.plusHours(2), response.getClosedAt());
    }
}
