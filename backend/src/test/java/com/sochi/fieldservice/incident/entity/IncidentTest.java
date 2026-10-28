package com.sochi.fieldservice.incident.entity;

import com.sochi.fieldservice.engineer.entity.Engineer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class IncidentTest {

    @Test
    void onCreateShouldInitializeTimestamps() {
        Incident incident = new Incident();
        assertNull(incident.getCreatedAt());
        assertNull(incident.getUpdatedAt());

        incident.onCreate();

        assertNotNull(incident.getCreatedAt());
        assertNotNull(incident.getUpdatedAt());
        assertEquals(incident.getCreatedAt(), incident.getUpdatedAt());
    }

    @Test
    void onCreateShouldInitializeSlaDeadlineBasedOnPriority() {
        Incident incident = new Incident();
        incident.setPriority(IncidentPriority.CRITICAL);

        incident.onCreate();

        assertNotNull(incident.getSlaDeadline());
        assertTrue(incident.getSlaDeadline().isAfter(incident.getCreatedAt()));
    }

    @Test
    void onUpdateShouldUpdateUpdatedAtTimestamp() throws InterruptedException {
        Incident incident = new Incident();
        incident.onCreate();
        LocalDateTime originalUpdatedAt = incident.getUpdatedAt();

        Thread.sleep(5);
        incident.onUpdate();

        assertNotNull(incident.getUpdatedAt());
        assertFalse(incident.getUpdatedAt().isBefore(originalUpdatedAt));
    }

    @Test
    @DisplayName("canTransitionTo should return false when status or target is null, or when target equals current status")
    void canTransitionToShouldReturnFalseForNullOrSameStatus() {
        Incident incident = new Incident();
        assertFalse(incident.canTransitionTo(IncidentStatus.OPEN));

        incident.setStatus(IncidentStatus.OPEN);
        assertFalse(incident.canTransitionTo(null));
        assertFalse(incident.canTransitionTo(IncidentStatus.OPEN));
    }

    @Test
    @DisplayName("Verify valid and invalid transitions from OPEN")
    void canTransitionToFromOpen() {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.OPEN);

        assertTrue(incident.canTransitionTo(IncidentStatus.ASSIGNED));
        assertTrue(incident.canTransitionTo(IncidentStatus.IN_PROGRESS));
        assertTrue(incident.canTransitionTo(IncidentStatus.CLOSED));
        assertFalse(incident.canTransitionTo(IncidentStatus.RESOLVED));
    }

    @Test
    @DisplayName("Verify valid and invalid transitions from ASSIGNED")
    void canTransitionToFromAssigned() {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.ASSIGNED);

        assertTrue(incident.canTransitionTo(IncidentStatus.IN_PROGRESS));
        assertTrue(incident.canTransitionTo(IncidentStatus.OPEN));
        assertTrue(incident.canTransitionTo(IncidentStatus.CLOSED));
        assertFalse(incident.canTransitionTo(IncidentStatus.RESOLVED));
    }

    @Test
    @DisplayName("Verify valid and invalid transitions from IN_PROGRESS")
    void canTransitionToFromInProgress() {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.IN_PROGRESS);

        assertTrue(incident.canTransitionTo(IncidentStatus.RESOLVED));
        assertTrue(incident.canTransitionTo(IncidentStatus.ASSIGNED));
        assertFalse(incident.canTransitionTo(IncidentStatus.OPEN));
        assertFalse(incident.canTransitionTo(IncidentStatus.CLOSED));
    }

    @Test
    @DisplayName("Verify valid and invalid transitions from RESOLVED")
    void canTransitionToFromResolved() {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.RESOLVED);

        assertTrue(incident.canTransitionTo(IncidentStatus.CLOSED));
        assertTrue(incident.canTransitionTo(IncidentStatus.IN_PROGRESS));
        assertFalse(incident.canTransitionTo(IncidentStatus.OPEN));
        assertFalse(incident.canTransitionTo(IncidentStatus.ASSIGNED));
    }

    @ParameterizedTest
    @EnumSource(IncidentStatus.class)
    @DisplayName("Verify CLOSED is a terminal state and cannot transition to any status")
    void canTransitionToFromClosedShouldAlwaysReturnFalse(IncidentStatus target) {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.CLOSED);

        assertFalse(incident.canTransitionTo(target));
    }

    @Test
    @DisplayName("Verify SLA calculation: Resolved before deadline is compliant")
    void shouldDetectResolvedBeforeDeadlineAsCompliant() {
        Incident incident = new Incident();
        LocalDateTime baseTime = LocalDateTime.now();
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setSlaDeadline(baseTime.plusHours(4));
        incident.setResolvedAt(baseTime.plusHours(2));

        assertFalse(incident.isSlaBreached());
    }

    @Test
    @DisplayName("Verify SLA calculation: Resolved after deadline is breached")
    void shouldDetectResolvedAfterDeadlineAsBreached() {
        Incident incident = new Incident();
        LocalDateTime baseTime = LocalDateTime.now();
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setSlaDeadline(baseTime.minusHours(4));
        incident.setResolvedAt(baseTime.minusHours(1));

        assertTrue(incident.isSlaBreached());
    }

    @Test
    @DisplayName("Verify SLA calculation: Active ticket overdue is breached")
    void shouldDetectActiveAndOverdueAsBreached() {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setSlaDeadline(LocalDateTime.now().minusMinutes(10));

        assertTrue(incident.isSlaBreached());
    }

    @Test
    @DisplayName("Verify SLA calculation: Active ticket within deadline is compliant")
    void shouldDetectActiveAndWithinDeadlineAsCompliant() {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.OPEN);
        incident.setSlaDeadline(LocalDateTime.now().plusHours(2));

        assertFalse(incident.isSlaBreached());
    }

    @Test
    @DisplayName("Verify SLA calculation: Null deadline returns false")
    void shouldReturnFalseWhenSlaDeadlineIsNull() {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.OPEN);
        incident.setSlaDeadline(null);

        assertFalse(incident.isSlaBreached());
    }

    @Test
    @DisplayName("Verify timestamps on status transitions: Resolved sets resolvedAt, Closed sets closedAt, Reopen clears them")
    void shouldManageResolvedAndClosedTimestampsAcrossTransitions() {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.OPEN);
        assertNull(incident.getResolvedAt());
        assertNull(incident.getClosedAt());

        // Transition to RESOLVED -> resolvedAt populated
        incident.setStatus(IncidentStatus.RESOLVED);
        assertNotNull(incident.getResolvedAt());
        assertNull(incident.getClosedAt());
        LocalDateTime firstResolutionTime = incident.getResolvedAt();

        // Transition to CLOSED -> closedAt populated
        incident.setStatus(IncidentStatus.CLOSED);
        assertEquals(firstResolutionTime, incident.getResolvedAt());
        assertNotNull(incident.getClosedAt());

        // Re-open ticket back to IN_PROGRESS -> both cleared
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        assertNull(incident.getResolvedAt());
        assertNull(incident.getClosedAt());
    }

    @Test
    @DisplayName("Verify getters, setters and associations")
    void shouldSupportGettersAndSetters() {
        Incident incident = new Incident();
        incident.setId(101L);
        incident.setLocation("North Tower");
        incident.setDescription("Fiber cut");
        LocalDateTime now = LocalDateTime.now();
        incident.setCreatedAt(now);
        incident.setUpdatedAt(now);
        Engineer engineer = new Engineer();
        engineer.setName("Test Engineer");
        incident.setAssignedEngineer(engineer);

        assertEquals(101L, incident.getId());
        assertEquals("North Tower", incident.getLocation());
        assertEquals("Fiber cut", incident.getDescription());
        assertEquals(now, incident.getCreatedAt());
        assertEquals(now, incident.getUpdatedAt());
        assertEquals("Test Engineer", incident.getAssignedEngineer().getName());
    }

    @Test
    @DisplayName("Verify version getter and setter for optimistic locking")
    void shouldSupportVersionGetterAndSetter() {
        Incident incident = new Incident();
        assertNull(incident.getVersion());
        incident.setVersion(3L);
        assertEquals(3L, incident.getVersion());
    }
}
