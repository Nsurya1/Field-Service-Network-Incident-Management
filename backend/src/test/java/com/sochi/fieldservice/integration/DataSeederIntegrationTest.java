package com.sochi.fieldservice.integration;

import com.sochi.fieldservice.config.DataSeeder;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentHistoryRepository;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataSeederIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EngineerRepository engineerRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentHistoryRepository historyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Verify DataSeeder seeds users, engineers, and 40 incidents, and is strictly idempotent")
    void shouldSeedDataAndMaintainIdempotency() {
        DataSeeder seeder = new DataSeeder(userRepository, engineerRepository, incidentRepository, historyRepository, passwordEncoder, true, "password123");

        // First execution - seed data
        seeder.run();

        // 1. Verify 3 users
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        assertTrue(adminOpt.isPresent(), "Admin user must be seeded");
        assertEquals(Role.ROLE_ADMIN, adminOpt.get().getRole());
        assertTrue(passwordEncoder.matches("password123", adminOpt.get().getPassword()));

        Optional<User> dispatcherOpt = userRepository.findByUsername("dispatcher");
        assertTrue(dispatcherOpt.isPresent(), "Dispatcher user must be seeded");
        assertEquals(Role.ROLE_MANAGER, dispatcherOpt.get().getRole());

        Optional<User> technicianOpt = userRepository.findByUsername("technician");
        assertTrue(technicianOpt.isPresent(), "Technician user must be seeded");
        assertEquals(Role.ROLE_ENGINEER, technicianOpt.get().getRole());

        // 2. Verify 8 engineers
        List<Engineer> engineers = engineerRepository.findAll();
        assertEquals(8, engineers.size(), "Exactly 8 field engineers must be seeded");
        assertTrue(engineers.stream().anyMatch(e -> "Fibre".equalsIgnoreCase(e.getSpecialization())));
        assertTrue(engineers.stream().anyMatch(e -> "Router".equalsIgnoreCase(e.getSpecialization())));
        assertTrue(engineers.stream().anyMatch(e -> "Wireless".equalsIgnoreCase(e.getSpecialization())));
        assertTrue(engineers.stream().anyMatch(e -> "Power".equalsIgnoreCase(e.getSpecialization())));

        // 3. Verify 40 incidents across statuses
        List<Incident> incidents = incidentRepository.findAll();
        assertEquals(40, incidents.size(), "Exactly 40 incidents must be seeded");

        long openCount = incidents.stream().filter(i -> i.getStatus() == IncidentStatus.OPEN).count();
        long assignedCount = incidents.stream().filter(i -> i.getStatus() == IncidentStatus.ASSIGNED).count();
        long inProgressCount = incidents.stream().filter(i -> i.getStatus() == IncidentStatus.IN_PROGRESS).count();
        long resolvedCount = incidents.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED).count();
        long closedCount = incidents.stream().filter(i -> i.getStatus() == IncidentStatus.CLOSED).count();

        assertEquals(8, openCount, "Must have 8 OPEN incidents");
        assertEquals(8, assignedCount, "Must have 8 ASSIGNED incidents");
        assertEquals(8, inProgressCount, "Must have 8 IN_PROGRESS incidents");
        assertEquals(8, resolvedCount, "Must have 8 RESOLVED incidents");
        assertEquals(8, closedCount, "Must have 8 CLOSED incidents");

        // 4. Verify SLA breaches present
        long breachedCount = incidents.stream().filter(Incident::isSlaBreached).count();
        assertTrue(breachedCount >= 8, "Expected at least 8 SLA breached incidents in seed data, found: " + breachedCount);

        // 5. Verify audit history exists
        assertTrue(historyRepository.count() >= 40, "Incident history entries must be recorded");

        // 6. Test Idempotency: second run must not create duplicates
        seeder.run();

        assertEquals(3, userRepository.count(), "User count must remain 3 after re-run");
        assertEquals(8, engineerRepository.count(), "Engineer count must remain 8 after re-run");
        assertEquals(40, incidentRepository.count(), "Incident count must remain 40 after re-run");
    }
}
