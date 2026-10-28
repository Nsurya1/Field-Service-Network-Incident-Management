package com.sochi.fieldservice.integration;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.IncidentService;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
import java.time.LocalDateTime;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import java.util.List;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IncidentIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EngineerRepository engineerRepository;

    @Test
    @DisplayName("Verify Spring context starts and connects to PostgreSQL test database")
    void shouldConnectToPostgreSqlTestDatabase() {
        String dbName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        assertEquals("fieldservice_test", dbName);
    }

    @Test
    @DisplayName("Verify incident can be created and persisted with auto-generated ID, timestamps and SLA deadline")
    void shouldCreateAndPersistIncident() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Fiber link severed");
        request.setDescription("Subsea fiber link issue detected");
        request.setPriority(IncidentPriority.CRITICAL);
        request.setLocation("Sector 4");

        Incident created = incidentService.createIncident(request);

        assertNotNull(created.getId());
        assertEquals("Fiber link severed", created.getTitle());
        assertEquals(IncidentStatus.OPEN, created.getStatus());
        assertNotNull(created.getCreatedAt(), "JPA @PrePersist should populate createdAt");
        assertNotNull(created.getUpdatedAt(), "JPA @PrePersist should populate updatedAt");
        assertNotNull(created.getSlaDeadline(), "SLA deadline should be populated and persisted");
        assertFalse(created.isSlaBreached());

        Incident retrieved = incidentRepository.findById(created.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals("Sector 4", retrieved.getLocation());
        assertNotNull(retrieved.getSlaDeadline());
    }

    @Test
    @DisplayName("Verify incident can be assigned to a persisted engineer and update status to ASSIGNED")
    void shouldAssignPersistedEngineerToIncident() {
        Engineer engineer = new Engineer();
        engineer.setName("Alice Sharma");
        engineer.setEmail("alice.sharma@example.com");
        engineer.setSpecialization("Fiber Optics");
        Engineer savedEngineer = engineerRepository.save(engineer);
        assertNotNull(savedEngineer.getId());
        assertNotNull(savedEngineer.getCreatedAt());

        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Switch port dead");
        request.setDescription("Port 12 on Switch 3 unresponsive");
        request.setPriority(IncidentPriority.HIGH);
        request.setLocation("Rack B-2");
        Incident savedIncident = incidentService.createIncident(request);

        Incident assigned = incidentService.assignIncident(savedIncident.getId(), savedEngineer.getId());

        assertEquals(IncidentStatus.ASSIGNED, assigned.getStatus());
        assertNotNull(assigned.getAssignedEngineer());
        assertEquals(savedEngineer.getId(), assigned.getAssignedEngineer().getId());
        assertEquals("Alice Sharma", assigned.getAssignedEngineer().getName());

        Incident fromDb = incidentRepository.findById(savedIncident.getId()).orElseThrow();
        assertEquals(savedEngineer.getId(), fromDb.getAssignedEngineer().getId());
    }

    @Test
    @DisplayName("Verify assigning incident to an engineer at max capacity throws 409 Conflict")
    void shouldRejectAssignmentWhenEngineerAtCapacity() {
        Engineer engineer = new Engineer();
        engineer.setName("Bob Dylan");
        engineer.setEmail("bob.dylan@example.com");
        engineer.setSpecialization("Core Routing");
        Engineer savedEngineer = engineerRepository.save(engineer);

        // Assign 3 active incidents (filling the max capacity limit of 3)
        for (int i = 1; i <= 3; i++) {
            CreateIncidentRequest req = new CreateIncidentRequest();
            req.setTitle("Incident " + i);
            req.setDescription("Description " + i);
            req.setPriority(IncidentPriority.HIGH);
            req.setLocation("Site " + i);
            Incident inc = incidentService.createIncident(req);
            incidentService.assignIncident(inc.getId(), savedEngineer.getId());
        }

        // Try assigning a 4th incident to the same engineer
        CreateIncidentRequest req4 = new CreateIncidentRequest();
        req4.setTitle("Incident 4");
        req4.setDescription("4th issue");
        req4.setPriority(IncidentPriority.MEDIUM);
        req4.setLocation("Site 4");
        Incident inc4 = incidentService.createIncident(req4);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.assignIncident(inc4.getId(), savedEngineer.getId())
        );

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("maximum active workload capacity"));
    }

    @Test
    @DisplayName("Verify status update follows valid lifecycle and persists correctly in PostgreSQL")
    void shouldUpdateIncidentStatusInDatabase() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("UPS Battery Warning");
        request.setDescription("Generator backup needs inspection");
        request.setPriority(IncidentPriority.MEDIUM);
        request.setLocation("Power Room");

        Incident incident = incidentService.createIncident(request);

        // Lifecycle: OPEN -> IN_PROGRESS -> RESOLVED
        incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.IN_PROGRESS);
        Incident updated = incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.RESOLVED);

        assertEquals(IncidentStatus.RESOLVED, updated.getStatus());

        Incident reloaded = incidentRepository.findById(incident.getId()).orElseThrow();
        assertEquals(IncidentStatus.RESOLVED, reloaded.getStatus());
    }

    @Test
    @DisplayName("Verify invalid status transition throws 400 Bad Request in integration test")
    void shouldRejectInvalidStatusTransitionInDatabase() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Power fluctuation");
        request.setDescription("Voltage spike");
        request.setPriority(IncidentPriority.LOW);
        request.setLocation("Server Room 1");

        Incident incident = incidentService.createIncident(request); // Status: OPEN

        // Attempt invalid jump: OPEN -> RESOLVED without work in progress
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.RESOLVED)
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    @DisplayName("Verify querying non-existent incident throws 404 ResponseStatusException")
    void shouldThrow404ForNonExistentIncident() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.getIncidentById(999999L)
        );
        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    @DisplayName("Verify incident lifecycle records chronological audit trail in PostgreSQL")
    void shouldRecordChronologicalAuditHistoryInDatabase() {
        // 1. Create incident -> 1 history entry (CREATED)
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Firewall firmware corruption");
        request.setDescription("Primary firewall dropped all packet forwarding");
        request.setPriority(IncidentPriority.CRITICAL);
        request.setLocation("Data Center 1");
        Incident incident = incidentService.createIncident(request);

        // 2. Assign engineer -> 2nd history entry (ASSIGNED)
        Engineer engineer = new Engineer();
        engineer.setName("Carol Danvers");
        engineer.setEmail("carol.danvers@example.com");
        engineer.setSpecialization("Firewalls");
        Engineer savedEngineer = engineerRepository.save(engineer);

        incidentService.assignIncident(incident.getId(), savedEngineer.getId());

        // 3. Update status -> 3rd history entry (STATUS_CHANGED: ASSIGNED -> IN_PROGRESS)
        incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.IN_PROGRESS);

        // 4. Update status -> 4th history entry (STATUS_CHANGED: IN_PROGRESS -> RESOLVED)
        incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.RESOLVED);

        // Retrieve audit trail from database
        List<IncidentHistory> history = incidentService.getIncidentHistory(incident.getId());

        assertEquals(4, history.size());
        assertEquals("CREATED", history.get(0).getAction());
        assertEquals("ASSIGNED", history.get(1).getAction());
        assertTrue(history.get(1).getDescription().contains("Carol Danvers"));
        assertEquals("STATUS_CHANGED", history.get(2).getAction());
        assertTrue(history.get(2).getDescription().contains("ASSIGNED to IN_PROGRESS"));
        assertEquals("STATUS_CHANGED", history.get(3).getAction());
        assertTrue(history.get(3).getDescription().contains("IN_PROGRESS to RESOLVED"));

        // Verify each history record has auto-generated ID, timestamp and links to incident
        for (IncidentHistory entry : history) {
            assertNotNull(entry.getId());
            assertNotNull(entry.getTimestamp());
            assertEquals(incident.getId(), entry.getIncident().getId());
        }
    }

    @Test
    @DisplayName("Verify incident statistics dashboard calculation in PostgreSQL")
    void shouldCalculateAccurateIncidentStatsInDatabase() {
        // 1. Create an OPEN incident with normal SLA
        CreateIncidentRequest req1 = new CreateIncidentRequest();
        req1.setTitle("Server room AC fail");
        req1.setDescription("Temperature rising");
        req1.setPriority(IncidentPriority.CRITICAL);
        req1.setLocation("Floor 2");
        Incident inc1 = incidentService.createIncident(req1);

        // 2. Create an ASSIGNED incident
        Engineer engineer = new Engineer();
        engineer.setName("Diana Prince");
        engineer.setEmail("diana.prince@example.com");
        engineer.setSpecialization("HVAC");
        Engineer savedEngineer = engineerRepository.save(engineer);

        CreateIncidentRequest req2 = new CreateIncidentRequest();
        req2.setTitle("Cooling fan dead");
        req2.setDescription("Rack 4 fan offline");
        req2.setPriority(IncidentPriority.HIGH);
        req2.setLocation("Floor 2");
        Incident inc2 = incidentService.createIncident(req2);
        incidentService.assignIncident(inc2.getId(), savedEngineer.getId());

        // 3. Create an IN_PROGRESS incident
        CreateIncidentRequest req3 = new CreateIncidentRequest();
        req3.setTitle("Database latency spike");
        req3.setDescription("Query timeouts");
        req3.setPriority(IncidentPriority.MEDIUM);
        req3.setLocation("Cloud VM");
        Incident inc3 = incidentService.createIncident(req3);
        incidentService.updateIncidentStatus(inc3.getId(), IncidentStatus.IN_PROGRESS);

        // 4. Create a RESOLVED incident
        CreateIncidentRequest req4 = new CreateIncidentRequest();
        req4.setTitle("Minor packet drop");
        req4.setDescription("Resolved by restart");
        req4.setPriority(IncidentPriority.LOW);
        req4.setLocation("Switch 5");
        Incident inc4 = incidentService.createIncident(req4);
        incidentService.updateIncidentStatus(inc4.getId(), IncidentStatus.IN_PROGRESS);
        incidentService.updateIncidentStatus(inc4.getId(), IncidentStatus.RESOLVED);

        // 5. Create an SLA-breached active incident (deadline in the past)
        CreateIncidentRequest req5 = new CreateIncidentRequest();
        req5.setTitle("Legacy switch down");
        req5.setDescription("Unresolved past SLA");
        req5.setPriority(IncidentPriority.CRITICAL);
        req5.setLocation("Old Annex");
        Incident inc5 = incidentService.createIncident(req5);
        inc5.setSlaDeadline(LocalDateTime.now().minusHours(5));
        incidentRepository.save(inc5);

        // Fetch stats
        IncidentStatsResponse stats = incidentService.getIncidentStats();

        assertNotNull(stats);
        assertTrue(stats.getTotalIncidents() >= 5);
        assertTrue(stats.getOpenIncidents() >= 2); // inc1 + inc5
        assertTrue(stats.getAssignedIncidents() >= 1); // inc2
        assertTrue(stats.getInProgressIncidents() >= 1); // inc3
        assertTrue(stats.getResolvedIncidents() >= 1); // inc4
        assertTrue(stats.getActiveIncidents() >= 4); // open + assigned + in_progress
        assertTrue(stats.getSlaBreachedIncidents() >= 1); // inc5 breached
        assertEquals(
                stats.getOpenIncidents() + stats.getAssignedIncidents() + stats.getInProgressIncidents(),
                stats.getActiveIncidents()
        );
    }

    @Test
    @DisplayName("Verify updatedAt changes in database and resolvedAt/closedAt lifecycle in PostgreSQL")
    void shouldUpdateTimestampsAndManageSlaLifecycleInDatabase() throws InterruptedException {
        // 1. Create incident
        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle("Core Switch Flapping");
        req.setDescription("BGP session drops");
        req.setPriority(IncidentPriority.CRITICAL);
        req.setLocation("Core PoP");
        Incident incident = incidentService.createIncident(req);
        Long id = incident.getId();

        LocalDateTime initialCreated = incident.getCreatedAt();
        LocalDateTime initialUpdated = incident.getUpdatedAt();
        assertNotNull(initialCreated);
        assertNotNull(initialUpdated);
        assertNull(incident.getResolvedAt());
        assertNull(incident.getClosedAt());

        // 2. Wait slightly, then advance status to IN_PROGRESS
        Thread.sleep(10);
        Incident inProgress = incidentService.updateIncidentStatus(id, IncidentStatus.IN_PROGRESS);
        LocalDateTime inProgressUpdated = inProgress.getUpdatedAt();
        assertFalse(inProgressUpdated.isBefore(initialUpdated));

        // 3. Move to RESOLVED -> resolvedAt populated
        Incident resolved = incidentService.updateIncidentStatus(id, IncidentStatus.RESOLVED);
        assertNotNull(resolved.getResolvedAt());
        assertNull(resolved.getClosedAt());

        // SLA deadline was in the future, resolved before deadline -> compliant
        assertFalse(resolved.isSlaBreached());

        // 4. Move to CLOSED -> closedAt populated
        Incident closed = incidentService.updateIncidentStatus(id, IncidentStatus.CLOSED);
        assertNotNull(closed.getResolvedAt());
        assertNotNull(closed.getClosedAt());
        assertFalse(closed.isSlaBreached());

        // 5. Test SLA breach on overdue resolution
        Incident overdue = new Incident();
        overdue.setTitle("Delayed restoration");
        overdue.setStatus(IncidentStatus.RESOLVED);
        overdue.setPriority(IncidentPriority.HIGH);
        overdue.setSlaDeadline(LocalDateTime.now().minusHours(2));
        overdue.setResolvedAt(LocalDateTime.now().minusHours(1)); // resolved after deadline
        Incident savedOverdue = incidentRepository.save(overdue);
        assertTrue(savedOverdue.isSlaBreached());

        // 6. Test re-open clears timestamps
        Incident reopened = new Incident();
        reopened.setTitle("Recurring Issue");
        reopened.setStatus(IncidentStatus.RESOLVED);
        reopened.setPriority(IncidentPriority.MEDIUM);
        reopened.setSlaDeadline(LocalDateTime.now().plusHours(10));
        reopened.setResolvedAt(LocalDateTime.now().minusMinutes(5));
        Incident savedReopened = incidentRepository.save(reopened);
        assertNotNull(savedReopened.getResolvedAt());

        // Reopen to IN_PROGRESS
        savedReopened.setStatus(IncidentStatus.IN_PROGRESS);
        Incident postReopen = incidentRepository.save(savedReopened);
        assertNull(postReopen.getResolvedAt());
        assertNull(postReopen.getClosedAt());
        assertFalse(postReopen.isSlaBreached());
    }
}
