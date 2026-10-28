package com.sochi.fieldservice.integration;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.IncidentService;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentHistoryRepository;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class IncidentConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EngineerRepository engineerRepository;

    @Autowired
    private IncidentHistoryRepository incidentHistoryRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        incidentHistoryRepository.deleteAll();
        incidentRepository.deleteAll();
        engineerRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify pessimistic lock prevents race conditions: 2 simultaneous assigns cannot exceed workload cap")
    void shouldPreventWorkloadCapBreachUnderConcurrentAssignments() throws Exception {
        // 1. Create engineer
        Engineer engineer = new Engineer();
        engineer.setName("Concurrent Field Tech");
        engineer.setEmail("concurrent.tech@example.com");
        engineer.setSpecialization("Core Concurrency");
        Engineer savedEngineer = engineerRepository.save(engineer);
        Long engineerId = savedEngineer.getId();

        // 2. Pre-assign 2 incidents so the engineer has active workload = 2 (cap is 3)
        for (int i = 1; i <= 2; i++) {
            CreateIncidentRequest req = new CreateIncidentRequest();
            req.setTitle("Pre-existing active ticket " + i);
            req.setDescription("Description " + i);
            req.setPriority(IncidentPriority.HIGH);
            Incident inc = incidentService.createIncident(req);
            incidentService.assignIncident(inc.getId(), engineerId);
        }

        long initialWorkload = incidentRepository.countByAssignedEngineerIdAndStatusIn(
                engineerId, List.of(IncidentStatus.ASSIGNED, IncidentStatus.IN_PROGRESS));
        assertEquals(2, initialWorkload);

        // 3. Create 2 new OPEN unassigned incidents
        CreateIncidentRequest reqA = new CreateIncidentRequest();
        reqA.setTitle("Concurrent Incident A");
        reqA.setDescription("Race condition test A");
        reqA.setPriority(IncidentPriority.CRITICAL);
        Incident incA = incidentService.createIncident(reqA);

        CreateIncidentRequest reqB = new CreateIncidentRequest();
        reqB.setTitle("Concurrent Incident B");
        reqB.setDescription("Race condition test B");
        reqB.setPriority(IncidentPriority.CRITICAL);
        Incident incB = incidentService.createIncident(reqB);

        // 4. Launch 2 concurrent threads trying to assign incA and incB simultaneously to the same engineer
        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(numThreads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        Long[] incidentIds = new Long[]{incA.getId(), incB.getId()};

        for (int i = 0; i < numThreads; i++) {
            final Long targetIncidentId = incidentIds[i];
            executor.submit(() -> {
                try {
                    startGate.await(); // ensure simultaneous execution
                    incidentService.assignIncident(targetIncidentId, engineerId);
                    successes.incrementAndGet();
                } catch (ResponseStatusException rse) {
                    if (rse.getStatusCode().value() == 409) {
                        conflicts.incrementAndGet();
                    } else {
                        unexpectedErrors.add(rse);
                    }
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    finishGate.countDown();
                }
            });
        }

        // Trigger both threads
        startGate.countDown();
        boolean completed = finishGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrent assignment threads must complete within timeout");
        assertTrue(unexpectedErrors.isEmpty(), "Unexpected errors during concurrent assignment: " + unexpectedErrors);

        // 5. Exactly 1 must succeed and exactly 1 must receive HTTP 409 Conflict
        assertEquals(1, successes.get(), "Exactly one concurrent assignment must succeed");
        assertEquals(1, conflicts.get(), "Exactly one concurrent assignment must fail with 409 Conflict");

        // 6. Verify database invariant: active workload is strictly 3 (never 4)
        long finalWorkload = incidentRepository.countByAssignedEngineerIdAndStatusIn(
                engineerId, List.of(IncidentStatus.ASSIGNED, IncidentStatus.IN_PROGRESS));
        assertEquals(3, finalWorkload, "Active workload must never exceed maximum cap of 3");
    }

    @Test
    @DisplayName("Verify @Version optimistic locking detects concurrent modifications on the same incident")
    void shouldDetectConcurrentModificationWithOptimisticLocking() {
        // Create an incident
        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle("Optimistic Locking Target");
        req.setDescription("Testing JPA @Version locking");
        req.setPriority(IncidentPriority.MEDIUM);
        Incident incident = incidentService.createIncident(req);
        Long incidentId = incident.getId();

        Long versionInDb = jdbcTemplate.queryForObject(
                "SELECT version FROM incident WHERE id = ?", Long.class, incidentId);
        assertEquals(0L, versionInDb);

        // Load entity with version 0
        Incident staleIncident = incidentRepository.findById(incidentId).orElseThrow();
        assertEquals(0L, staleIncident.getVersion());

        // Simulate concurrent modification in another transaction (increments version in database to 1)
        jdbcTemplate.update("UPDATE incident SET version = version + 1, description = 'Updated concurrently' WHERE id = ?", incidentId);

        Long updatedVersionInDb = jdbcTemplate.queryForObject(
                "SELECT version FROM incident WHERE id = ?", Long.class, incidentId);
        assertEquals(1L, updatedVersionInDb);

        // Attempting to save the stale entity (with version = 0) must trigger OptimisticLockingFailureException
        staleIncident.setDescription("Conflicting update from stale client");
        assertThrows(OptimisticLockingFailureException.class, () -> {
            incidentRepository.saveAndFlush(staleIncident);
        });
    }
}
