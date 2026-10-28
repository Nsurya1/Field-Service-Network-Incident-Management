package com.sochi.fieldservice.engineer.service;

import com.sochi.fieldservice.engineer.dto.CreateEngineerRequest;
import com.sochi.fieldservice.engineer.dto.EngineerResponse;
import com.sochi.fieldservice.engineer.dto.UpdateEngineerRequest;
import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EngineerServiceTest {

    @Mock
    private EngineerRepository engineerRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private EngineerService engineerService;

    @Test
    @DisplayName("Verify createEngineer throws 409 Conflict when email already exists")
    void createEngineerShouldThrow409WhenEmailAlreadyExists() {
        CreateEngineerRequest request = new CreateEngineerRequest("Test Engineer", "TEST@example.com", "Network Support");

        when(engineerRepository.existsByEmail("test@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> engineerService.createEngineer(request)
        );

        assertEquals(HttpStatus.CONFLICT.value(), exception.getStatusCode().value());
        assertEquals("An engineer with this email already exists", exception.getReason());
        verify(engineerRepository, never()).save(any(Engineer.class));
    }

    @Test
    @DisplayName("Verify createEngineer saves engineer and returns EngineerResponse")
    void createEngineerShouldSaveEngineerWhenEmailIsNew() {
        CreateEngineerRequest request = new CreateEngineerRequest(
                "Test Engineer", "TEST@example.com", "Network Support", "+91-98765-43210"
        );

        when(engineerRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(engineerRepository.save(any(Engineer.class)))
                .thenAnswer(invocation -> {
                    Engineer eng = invocation.getArgument(0);
                    ReflectionTestUtils.setField(eng, "id", 1L);
                    ReflectionTestUtils.setField(eng, "createdAt", LocalDateTime.now());
                    return eng;
                });

        EngineerResponse result = engineerService.createEngineer(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Engineer", result.getName());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Network Support", result.getSpecialization());
        assertEquals("+91-98765-43210", result.getPhone());
        assertTrue(result.isActive());
        assertEquals(AvailabilityStatus.AVAILABLE, result.getAvailabilityStatus());
        assertEquals(0L, result.getCurrentWorkload());

        verify(engineerRepository).save(any(Engineer.class));
    }

    @Test
    @DisplayName("Verify getAllEngineers returns list of EngineerResponse with calculated workload")
    void shouldReturnAllEngineersWithWorkload() {
        Engineer engineer1 = new Engineer();
        ReflectionTestUtils.setField(engineer1, "id", 1L);
        engineer1.setName("Rahul Sharma");
        engineer1.setEmail("rahul@example.com");
        engineer1.setSpecialization("Networking");

        Engineer engineer2 = new Engineer();
        ReflectionTestUtils.setField(engineer2, "id", 2L);
        engineer2.setName("Amit Verma");
        engineer2.setEmail("amit@example.com");
        engineer2.setSpecialization("Hardware");

        when(engineerRepository.findAll()).thenReturn(List.of(engineer1, engineer2));
        when(incidentRepository.countByAssignedEngineerIdAndStatusIn(eq(1L), anyList())).thenReturn(2L);
        when(incidentRepository.countByAssignedEngineerIdAndStatusIn(eq(2L), anyList())).thenReturn(0L);

        List<EngineerResponse> engineers = engineerService.getAllEngineers();

        assertEquals(2, engineers.size());
        assertEquals("Rahul Sharma", engineers.get(0).getName());
        assertEquals(2L, engineers.get(0).getCurrentWorkload());
        assertEquals("Amit Verma", engineers.get(1).getName());
        assertEquals(0L, engineers.get(1).getCurrentWorkload());

        verify(engineerRepository).findAll();
    }

    @Test
    @DisplayName("Verify getEngineerResponseById returns EngineerResponse")
    void shouldReturnEngineerResponseById() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 1L);
        engineer.setName("Rahul Sharma");
        engineer.setEmail("rahul@example.com");
        engineer.setSpecialization("Networking");

        when(engineerRepository.findById(1L)).thenReturn(Optional.of(engineer));
        when(incidentRepository.countByAssignedEngineerIdAndStatusIn(eq(1L), anyList())).thenReturn(1L);

        EngineerResponse result = engineerService.getEngineerResponseById(1L);

        assertEquals("Rahul Sharma", result.getName());
        assertEquals("rahul@example.com", result.getEmail());
        assertEquals(1L, result.getCurrentWorkload());
    }

    @Test
    @DisplayName("Verify updateEngineer updates mutable fields and returns EngineerResponse")
    void shouldUpdateEngineerDetails() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 1L);
        engineer.setName("Old Name");
        engineer.setEmail("engineer@example.com");
        engineer.setSpecialization("Old Spec");
        engineer.setPhone("+91-00000");

        UpdateEngineerRequest request = new UpdateEngineerRequest(
                "New Name", "+91-99999", "New Spec", AvailabilityStatus.BUSY
        );

        when(engineerRepository.findById(1L)).thenReturn(Optional.of(engineer));
        when(engineerRepository.save(any(Engineer.class))).thenAnswer(i -> i.getArgument(0));
        when(incidentRepository.countByAssignedEngineerIdAndStatusIn(eq(1L), anyList())).thenReturn(3L);

        EngineerResponse updated = engineerService.updateEngineer(1L, request);

        assertEquals("New Name", updated.getName());
        assertEquals("New Spec", updated.getSpecialization());
        assertEquals("+91-99999", updated.getPhone());
        assertEquals(AvailabilityStatus.BUSY, updated.getAvailabilityStatus());
        assertEquals(3L, updated.getCurrentWorkload());
        verify(engineerRepository).save(engineer);
    }

    @Test
    @DisplayName("Verify updateEngineerActiveStatus updates active flag and returns EngineerResponse")
    void shouldUpdateEngineerActiveStatus() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 5L);
        engineer.setName("Field Eng");
        engineer.setEmail("field@example.com");
        engineer.setActive(true);

        when(engineerRepository.findById(5L)).thenReturn(Optional.of(engineer));
        when(engineerRepository.save(any(Engineer.class))).thenAnswer(i -> i.getArgument(0));
        when(incidentRepository.countByAssignedEngineerIdAndStatusIn(eq(5L), anyList())).thenReturn(0L);

        EngineerResponse updated = engineerService.updateEngineerActiveStatus(5L, false);

        assertFalse(updated.isActive());
        verify(engineerRepository).save(engineer);
    }

    @Test
    @DisplayName("Verify getEngineerById returns Engineer entity")
    void shouldReturnEngineerById() {
        Engineer engineer = new Engineer();
        engineer.setName("Rahul Sharma");
        engineer.setEmail("rahul@example.com");

        when(engineerRepository.findById(1L)).thenReturn(Optional.of(engineer));

        Engineer result = engineerService.getEngineerById(1L);

        assertEquals("Rahul Sharma", result.getName());
        assertEquals("rahul@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Verify getEngineerByIdForUpdate uses pessimistic lock query")
    void shouldReturnEngineerByIdForUpdate() {
        Engineer engineer = new Engineer();
        engineer.setName("Pessimistic Eng");

        when(engineerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(engineer));

        Engineer result = engineerService.getEngineerByIdForUpdate(2L);

        assertEquals("Pessimistic Eng", result.getName());
        verify(engineerRepository).findByIdForUpdate(2L);
    }

    @Test
    @DisplayName("Verify getEngineerById throws 404 when engineer does not exist")
    void shouldThrow404WhenEngineerDoesNotExist() {
        when(engineerRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> engineerService.getEngineerById(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Engineer not found with ID: 999", exception.getReason());
    }

    @Test
    @DisplayName("Verify getEngineerByIdForUpdate throws 404 when engineer does not exist")
    void shouldThrow404WhenEngineerForUpdateDoesNotExist() {
        when(engineerRepository.findByIdForUpdate(888L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> engineerService.getEngineerByIdForUpdate(888L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Engineer not found with ID: 888", exception.getReason());
    }
}
