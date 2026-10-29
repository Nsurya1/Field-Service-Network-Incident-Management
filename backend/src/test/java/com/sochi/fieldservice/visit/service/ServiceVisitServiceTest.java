package com.sochi.fieldservice.visit.service;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.service.EngineerService;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.incident.repository.IncidentHistoryRepository;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import com.sochi.fieldservice.visit.dto.CreateServiceVisitRequest;
import com.sochi.fieldservice.visit.dto.ServiceVisitResponse;
import com.sochi.fieldservice.visit.entity.ServiceVisit;
import com.sochi.fieldservice.visit.repository.ServiceVisitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceVisitServiceTest {

    @Mock
    private ServiceVisitRepository serviceVisitRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private EngineerService engineerService;

    @Mock
    private IncidentHistoryRepository incidentHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ServiceVisitService serviceVisitService;

    @Test
    @DisplayName("Verify createVisit succeeds with explicit engineerId")
    void createVisitWithExplicitEngineerIdShouldSucceed() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);

        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 2L);
        engineer.setName("Arun Field");

        CreateServiceVisitRequest request = new CreateServiceVisitRequest(
                2L, LocalDate.of(2026, 10, 29),
                LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                "Inspected OLT chassis", "Cleaned connector"
        );

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(engineerService.getEngineerById(2L)).thenReturn(engineer);
        when(serviceVisitRepository.save(any(ServiceVisit.class))).thenAnswer(i -> {
            ServiceVisit v = i.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 100L);
            ReflectionTestUtils.setField(v, "createdAt", LocalDateTime.now());
            return v;
        });

        ServiceVisitResponse response = serviceVisitService.createVisit(1L, request, null);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(1L, response.getIncidentId());
        assertEquals(2L, response.getEngineerId());
        assertEquals("Arun Field", response.getEngineerName());
        assertEquals("Inspected OLT chassis", response.getWorkNotes());
        assertEquals("Cleaned connector", response.getResolutionNotes());

        verify(serviceVisitRepository).save(any(ServiceVisit.class));
        verify(incidentHistoryRepository).save(any(IncidentHistory.class));
    }

    @Test
    @DisplayName("Verify createVisit falls back to user engineer when engineerId not supplied")
    void createVisitWithUserEngineerFallbackShouldSucceed() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);

        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 3L);
        engineer.setName("User Tech");
        incident.setAssignedEngineer(engineer);

        User user = new User("user_tech", "tech@example.com", "pass", Role.ROLE_ENGINEER, engineer);

        CreateServiceVisitRequest request = new CreateServiceVisitRequest("Replaced jumper cable");

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("user_tech")).thenReturn(Optional.of(user));
        when(serviceVisitRepository.save(any(ServiceVisit.class))).thenAnswer(i -> {
            ServiceVisit v = i.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 101L);
            return v;
        });

        ServiceVisitResponse response = serviceVisitService.createVisit(1L, request, "user_tech");

        assertNotNull(response);
        assertEquals(3L, response.getEngineerId());
        assertEquals("User Tech", response.getEngineerName());
    }

    @Test
    @DisplayName("Verify createVisit falls back to assigned engineer when engineerId and username not supplied")
    void createVisitWithAssignedEngineerFallbackShouldSucceed() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);

        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 4L);
        engineer.setName("Assigned Tech");
        incident.setAssignedEngineer(engineer);

        CreateServiceVisitRequest request = new CreateServiceVisitRequest("Fixed power supply");

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(serviceVisitRepository.save(any(ServiceVisit.class))).thenAnswer(i -> i.getArgument(0));

        ServiceVisitResponse response = serviceVisitService.createVisit(1L, request, null);

        assertNotNull(response);
        assertEquals(4L, response.getEngineerId());
        assertEquals("Assigned Tech", response.getEngineerName());
    }

    @Test
    @DisplayName("Verify createVisit throws 404 when incident does not exist")
    void createVisitShouldThrow404WhenIncidentNotFound() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        CreateServiceVisitRequest request = new CreateServiceVisitRequest("Test notes");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> serviceVisitService.createVisit(999L, request, null));

        assertEquals(404, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Incident not found"));
    }

    @Test
    @DisplayName("Verify createVisit throws 400 when no engineer can be resolved")
    void createVisitShouldThrow400WhenNoEngineerAvailable() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);
        incident.setAssignedEngineer(null);

        CreateServiceVisitRequest request = new CreateServiceVisitRequest("Test notes");

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> serviceVisitService.createVisit(1L, request, null));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("An engineer must be specified or assigned"));
    }

    @Test
    @DisplayName("Verify createVisit throws 403 when authenticated engineer attempts visit on unassigned incident")
    void createVisitShouldThrow403WhenEngineerNotAssigned() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);

        Engineer eng1 = new Engineer();
        ReflectionTestUtils.setField(eng1, "id", 10L);
        incident.setAssignedEngineer(eng1);

        Engineer eng2 = new Engineer();
        ReflectionTestUtils.setField(eng2, "id", 20L);
        User tech2 = new User("tech2", "tech2@test.com", "pass", Role.ROLE_ENGINEER, eng2);

        CreateServiceVisitRequest request = new CreateServiceVisitRequest(10L, null, null, null, "Illegal visit notes", null);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("tech2")).thenReturn(Optional.of(tech2));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> serviceVisitService.createVisit(1L, request, "tech2"));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Engineers can only log service visits on incidents assigned to them"));
    }

    @Test
    @DisplayName("Verify getVisitsForIncident returns list of ServiceVisitResponse")
    void getVisitsForIncidentShouldReturnVisits() {
        when(incidentRepository.existsById(1L)).thenReturn(true);

        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 2L);

        ServiceVisit v1 = new ServiceVisit(incident, engineer, LocalDate.now(), "Notes 1");
        ServiceVisit v2 = new ServiceVisit(incident, engineer, LocalDate.now(), "Notes 2");

        when(serviceVisitRepository.findByIncidentIdOrderByVisitDateAscCheckInAtAscCreatedAtAsc(1L))
                .thenReturn(List.of(v1, v2));

        List<ServiceVisitResponse> result = serviceVisitService.getVisitsForIncident(1L);

        assertEquals(2, result.size());
        assertEquals("Notes 1", result.get(0).getWorkNotes());
        assertEquals("Notes 2", result.get(1).getWorkNotes());
    }

    @Test
    @DisplayName("Verify getVisitsForIncident throws 404 when incident does not exist")
    void getVisitsForIncidentShouldThrow404WhenIncidentNotFound() {
        when(incidentRepository.existsById(999L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> serviceVisitService.getVisitsForIncident(999L));

        assertEquals(404, ex.getStatusCode().value());
    }
}
