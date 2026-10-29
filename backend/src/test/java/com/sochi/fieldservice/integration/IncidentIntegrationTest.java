package com.sochi.fieldservice.integration;

import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.IncidentService;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.incident.entity.RootCause;
import java.util.List;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.visit.dto.CreateServiceVisitRequest;
import com.sochi.fieldservice.visit.dto.ServiceVisitResponse;
import com.sochi.fieldservice.visit.service.ServiceVisitService;
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

    @Autowired
    private ServiceVisitService serviceVisitService;

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
        Engineer engineer = new Engineer();
        engineer.setName("Status Lifecycle Tech");
        engineer.setEmail("lifecycle.tech@example.com");
        engineer.setSpecialization("Core Network");
        Engineer savedEngineer = engineerRepository.save(engineer);

        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("UPS Battery Warning");
        request.setDescription("Generator backup needs inspection");
        request.setPriority(IncidentPriority.MEDIUM);
        request.setLocation("Power Room");

        Incident incident = incidentService.createIncident(request);
        incidentService.assignIncident(incident.getId(), savedEngineer.getId());

        // Lifecycle: ASSIGNED -> IN_PROGRESS -> RESOLVED
        incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.IN_PROGRESS);
        Incident updated = incidentService.updateIncidentStatus(
                incident.getId(),
                IncidentStatus.RESOLVED,
                "Replaced faulty optical fiber link",
                RootCause.FIBER_CUT
        );

        assertEquals(IncidentStatus.RESOLVED, updated.getStatus());
        assertEquals("Replaced faulty optical fiber link", updated.getResolutionNotes());
        assertEquals(RootCause.FIBER_CUT, updated.getRootCause());

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
        incidentService.updateIncidentStatus(
                incident.getId(),
                IncidentStatus.RESOLVED,
                "Resolved fiber attenuation",
                RootCause.FIBER_CUT
        );

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
        incidentService.assignIncident(inc3.getId(), savedEngineer.getId());
        incidentService.updateIncidentStatus(inc3.getId(), IncidentStatus.IN_PROGRESS);

        // 4. Create a RESOLVED incident
        CreateIncidentRequest req4 = new CreateIncidentRequest();
        req4.setTitle("Minor packet drop");
        req4.setDescription("Resolved by restart");
        req4.setPriority(IncidentPriority.LOW);
        req4.setLocation("Switch 5");
        Incident inc4 = incidentService.createIncident(req4);
        incidentService.assignIncident(inc4.getId(), savedEngineer.getId());
        incidentService.updateIncidentStatus(inc4.getId(), IncidentStatus.IN_PROGRESS);
        incidentService.updateIncidentStatus(
                inc4.getId(),
                IncidentStatus.RESOLVED,
                "Packet drop resolved by restart and reload",
                RootCause.OTHER
        );

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

        Engineer eng = new Engineer();
        eng.setName("Timestamp Lifecycle Engineer");
        eng.setEmail("timestamplifecycle@example.com");
        eng.setSpecialization("Core Switch");
        Engineer savedEng = engineerRepository.save(eng);
        incidentService.assignIncident(id, savedEng.getId());

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
        Incident resolved = incidentService.updateIncidentStatus(
                id,
                IncidentStatus.RESOLVED,
                "BGP peering dampening configured and cleared",
                RootCause.CONFIGURATION_ERROR
        );
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

    @Test
    @DisplayName("Verify unassign resets status to OPEN, removes engineer, and appends UNASSIGNED audit history")
    void shouldUnassignIncidentAndResetToOpenInDatabase() {
        Engineer engineer = new Engineer();
        engineer.setName("Unassign Target Eng");
        engineer.setEmail("unassign.target@example.com");
        engineer.setSpecialization("Optical");
        Engineer savedEngineer = engineerRepository.save(engineer);

        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle("DWDM Link Degraded");
        req.setDescription("Optical power dropping");
        req.setPriority(IncidentPriority.HIGH);
        req.setLocation("Span 12");
        Incident incident = incidentService.createIncident(req);

        incidentService.assignIncident(incident.getId(), savedEngineer.getId());
        assertEquals(IncidentStatus.ASSIGNED, incidentRepository.findById(incident.getId()).orElseThrow().getStatus());

        Incident unassigned = incidentService.unassignIncident(incident.getId());
        assertEquals(IncidentStatus.OPEN, unassigned.getStatus());
        assertNull(unassigned.getAssignedEngineer());

        Incident fromDb = incidentRepository.findById(incident.getId()).orElseThrow();
        assertEquals(IncidentStatus.OPEN, fromDb.getStatus());
        assertNull(fromDb.getAssignedEngineer());

        List<IncidentHistory> history = incidentService.getIncidentHistory(incident.getId());
        assertEquals(3, history.size()); // CREATED, ASSIGNED, UNASSIGNED
        assertEquals("UNASSIGNED", history.get(2).getAction());
        assertTrue(history.get(2).getDescription().contains("Unassign Target Eng"));
    }

    @Test
    @DisplayName("Verify transitioning to IN_PROGRESS without assigned engineer is blocked with HTTP 400")
    void shouldBlockInProgressTransitionWhenNoEngineerAssigned() {
        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle("Unassigned ticket test");
        req.setDescription("No technician assigned");
        req.setPriority(IncidentPriority.LOW);
        Incident incident = incidentService.createIncident(req);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.IN_PROGRESS));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Cannot transition to IN_PROGRESS without an assigned engineer"));
    }

    @Test
    @DisplayName("Verify transitioning to RESOLVED without assigned engineer is blocked with HTTP 400")
    void shouldBlockResolvedTransitionWhenNoEngineerAssigned() {
        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle("Unassigned ticket test 2");
        req.setDescription("No technician assigned");
        req.setPriority(IncidentPriority.LOW);
        Incident incident = incidentService.createIncident(req);

        // Set status to IN_PROGRESS directly without engineer
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incidentRepository.save(incident);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.RESOLVED));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Cannot transition to RESOLVED without an assigned engineer"));
    }

    @Test
    @DisplayName("Verify assigning already IN_PROGRESS ticket to new engineer retains IN_PROGRESS status")
    void shouldRetainInProgressStatusWhenReassigningIncident() {
        Engineer eng1 = new Engineer();
        eng1.setName("First Tech");
        eng1.setEmail("first.tech@example.com");
        eng1.setSpecialization("Field");
        Engineer savedEng1 = engineerRepository.save(eng1);

        Engineer eng2 = new Engineer();
        eng2.setName("Second Tech");
        eng2.setEmail("second.tech@example.com");
        eng2.setSpecialization("Field");
        Engineer savedEng2 = engineerRepository.save(eng2);

        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle("Substation link failure");
        req.setDescription("Radio link misaligned");
        req.setPriority(IncidentPriority.HIGH);
        Incident incident = incidentService.createIncident(req);

        incidentService.assignIncident(incident.getId(), savedEng1.getId());
        incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.IN_PROGRESS);

        // Reassign to eng2
        Incident reassigned = incidentService.assignIncident(incident.getId(), savedEng2.getId());

        assertEquals(IncidentStatus.IN_PROGRESS, reassigned.getStatus(), "Status must remain IN_PROGRESS on reassignment");
        assertEquals(savedEng2.getId(), reassigned.getAssignedEngineer().getId());
    }

    @Test
    @DisplayName("Verify assigning incident to an inactive engineer is blocked with HTTP 400")
    void shouldBlockAssignmentToInactiveEngineer() {
        Engineer inactiveEng = new Engineer();
        inactiveEng.setName("Inactive Integration Tech");
        inactiveEng.setEmail("inactive.integration@test.com");
        inactiveEng.setSpecialization("Core");
        inactiveEng.setActive(false);
        Engineer savedEng = engineerRepository.save(inactiveEng);

        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle("Inactive assignment target");
        req.setDescription("Desc");
        req.setPriority(IncidentPriority.MEDIUM);
        Incident incident = incidentService.createIncident(req);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.assignIncident(incident.getId(), savedEng.getId()));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("inactive"));
    }

    @Test
    @DisplayName("Verify assigning incident to an off-duty engineer is blocked with HTTP 400")
    void shouldBlockAssignmentToOffDutyEngineer() {
        Engineer offDutyEng = new Engineer();
        offDutyEng.setName("OffDuty Integration Tech");
        offDutyEng.setEmail("offduty.integration@test.com");
        offDutyEng.setSpecialization("Core");
        offDutyEng.setActive(true);
        offDutyEng.setAvailabilityStatus(AvailabilityStatus.OFF_DUTY);
        Engineer savedEng = engineerRepository.save(offDutyEng);

        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle("OffDuty assignment target");
        req.setDescription("Desc");
        req.setPriority(IncidentPriority.MEDIUM);
        Incident incident = incidentService.createIncident(req);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.assignIncident(incident.getId(), savedEng.getId()));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("off-duty"));
    }

    @Test
    @DisplayName("Verify moving incident to RESOLVED without resolution notes is blocked with HTTP 400")
    void shouldBlockResolutionWithoutResolutionNotesInDatabase() {
        Engineer eng = new Engineer();
        eng.setName("Resolution Tech");
        eng.setEmail("res.tech@test.com");
        eng.setSpecialization("Core");
        Engineer savedEng = engineerRepository.save(eng);

        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle("Test Resolution Notes Validation");
        req.setDescription("Desc");
        req.setPriority(IncidentPriority.HIGH);
        Incident incident = incidentService.createIncident(req);
        incidentService.assignIncident(incident.getId(), savedEng.getId());
        incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.IN_PROGRESS);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(incident.getId(), IncidentStatus.RESOLVED));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Resolution notes of at least 10 characters are required"));
    }

    @Test
    @DisplayName("Verify creating and retrieving service visits in PostgreSQL database")
    void shouldCreateAndRetrieveServiceVisitsInDatabase() {
        Engineer eng = new Engineer();
        eng.setName("On-site Visit Engineer");
        eng.setEmail("visit.engineer@test.com");
        eng.setSpecialization("Fiber");
        Engineer savedEng = engineerRepository.save(eng);

        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle("Core POP Fiber Cut");
        req.setDescription("Physical cable cut along transit route");
        req.setPriority(IncidentPriority.CRITICAL);
        Incident incident = incidentService.createIncident(req);
        incidentService.assignIncident(incident.getId(), savedEng.getId());

        CreateServiceVisitRequest visitReq = new CreateServiceVisitRequest();
        visitReq.setEngineerId(savedEng.getId());
        visitReq.setVisitDate(LocalDate.now());
        visitReq.setCheckInAt(LocalDateTime.now().minusHours(3));
        visitReq.setCheckOutAt(LocalDateTime.now().minusMinutes(30));
        visitReq.setWorkNotes("Splice enclosure replaced; OTDR test passes at -18 dBm");
        visitReq.setResolutionNotes("Fiber connection restored and signal verified");

        ServiceVisitResponse visitResponse = serviceVisitService.createVisit(incident.getId(), visitReq, null);

        assertNotNull(visitResponse.getId());
        assertEquals(incident.getId(), visitResponse.getIncidentId());
        assertEquals(savedEng.getId(), visitResponse.getEngineerId());
        assertEquals("On-site Visit Engineer", visitResponse.getEngineerName());
        assertEquals("Splice enclosure replaced; OTDR test passes at -18 dBm", visitResponse.getWorkNotes());
        assertEquals("Fiber connection restored and signal verified", visitResponse.getResolutionNotes());

        List<ServiceVisitResponse> visits = serviceVisitService.getVisitsForIncident(incident.getId());
        assertEquals(1, visits.size());
        assertEquals(visitResponse.getId(), visits.get(0).getId());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_visits WHERE incident_id = ?",
                Integer.class,
                incident.getId()
        );
        assertEquals(1, count);
    }
}
