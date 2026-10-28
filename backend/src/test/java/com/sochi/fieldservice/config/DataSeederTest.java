package com.sochi.fieldservice.config;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.incident.repository.IncidentHistoryRepository;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EngineerRepository engineerRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentHistoryRepository historyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        dataSeeder = new DataSeeder(userRepository, engineerRepository, incidentRepository, historyRepository, passwordEncoder);
        dataSeeder.setSeedEnabled(true);
        dataSeeder.setDefaultPassword("customPass123");
    }

    @Test
    @DisplayName("When app.seed.enabled is false, seeder exits immediately without repository interactions")
    void shouldSkipSeedingWhenDisabled() {
        ReflectionTestUtils.setField(dataSeeder, "seedEnabled", false);

        dataSeeder.run();

        verifyNoInteractions(userRepository, engineerRepository, incidentRepository, historyRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Seeds users, 8 engineers, and 40 incidents when database is completely empty")
    void shouldSeedAllDataWhenDatabaseIsEmpty() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("customPass123")).thenReturn("encodedHash123");
        when(engineerRepository.count()).thenReturn(0L);

        List<Engineer> mockEngineers = new ArrayList<>();
        for (long i = 1; i <= 8; i++) {
            Engineer eng = new Engineer();
            eng.setName("Engineer " + i);
            eng.setEmail("engineer" + i + "@telecom.net");
            eng.setSpecialization(i % 2 == 0 ? "Router" : "Fibre");
            mockEngineers.add(eng);
        }
        when(engineerRepository.saveAll(anyList())).thenReturn(mockEngineers);
        when(incidentRepository.count()).thenReturn(0L);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dataSeeder.run();

        // 1. Verify 4 users seeded
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(4)).save(userCaptor.capture());
        List<User> savedUsers = userCaptor.getAllValues();
        assertEquals(4, savedUsers.size());
        assertTrue(savedUsers.stream().anyMatch(u -> u.getUsername().equals("admin") && u.getRole() == Role.ROLE_ADMIN));
        assertTrue(savedUsers.stream().anyMatch(u -> u.getUsername().equals("dispatcher") && u.getRole() == Role.ROLE_MANAGER));
        assertTrue(savedUsers.stream().anyMatch(u -> u.getUsername().equals("technician") && u.getRole() == Role.ROLE_ENGINEER));
        assertTrue(savedUsers.stream().anyMatch(u -> u.getUsername().equals("viewer") && u.getRole() == Role.ROLE_VIEWER));

        // 2. Verify 8 engineers seeded
        verify(engineerRepository).saveAll(anyList());

        // 3. Verify 40 incidents saved
        verify(incidentRepository, times(40)).save(any(Incident.class));

        // 4. Verify history saved
        verify(historyRepository, atLeast(40)).save(any(IncidentHistory.class));
    }

    @Test
    @DisplayName("Idempotency: When data is already present, seeder skips existing records without duplicating")
    void shouldBeIdempotentWhenDataAlreadyExists() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("dispatcher")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("technician")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(new User()));
        when(engineerRepository.count()).thenReturn(8L);
        when(incidentRepository.count()).thenReturn(40L);

        dataSeeder.run();

        verify(userRepository, never()).save(any(User.class));
        verify(engineerRepository, never()).saveAll(anyList());
        verify(incidentRepository, never()).save(any(Incident.class));
        verify(historyRepository, never()).save(any(IncidentHistory.class));
    }

    @Test
    @DisplayName("When engineers count > 0 but incidents empty, reuses existing engineers for incidents")
    void shouldReuseExistingEngineersWhenIncidentsNeedSeeding() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(new User()));
        when(engineerRepository.count()).thenReturn(8L);

        List<Engineer> mockEngineers = new ArrayList<>();
        for (long i = 1; i <= 8; i++) {
            Engineer eng = new Engineer();
            eng.setName("Existing Engineer " + i);
            mockEngineers.add(eng);
        }
        when(engineerRepository.findAll()).thenReturn(mockEngineers);
        when(incidentRepository.count()).thenReturn(0L);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dataSeeder.run();

        verify(incidentRepository, times(40)).save(any(Incident.class));
    }

    @Test
    @DisplayName("Verify getters, setters and overloaded constructor")
    void testGettersSettersAndOverloadedConstructor() {
        DataSeeder seeder = new DataSeeder(userRepository, engineerRepository, incidentRepository, historyRepository, passwordEncoder, false, "secret");
        assertFalse(seeder.isSeedEnabled());
        assertEquals("secret", seeder.getDefaultPassword());

        seeder.setSeedEnabled(true);
        seeder.setDefaultPassword("newPass");
        assertTrue(seeder.isSeedEnabled());
        assertEquals("newPass", seeder.getDefaultPassword());
    }
}
