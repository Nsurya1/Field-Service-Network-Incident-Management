package com.sochi.fieldservice.incident.service;

import com.sochi.fieldservice.engineer.service.EngineerService;

import com.sochi.fieldservice.incident.IncidentService;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.dto.IncidentFilterCriteria;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
import com.sochi.fieldservice.incident.dto.UpdateIncidentRequest;
import org.springframework.data.jpa.domain.Specification;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.repository.IncidentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.sochi.fieldservice.incident.dto.CreateIncidentCommentRequest;
import com.sochi.fieldservice.incident.dto.IncidentCommentResponse;
import com.sochi.fieldservice.incident.dto.TimelineItemResponse;
import com.sochi.fieldservice.incident.entity.IncidentComment;
import com.sochi.fieldservice.incident.repository.IncidentCommentRepository;
import com.sochi.fieldservice.visit.entity.ServiceVisit;
import com.sochi.fieldservice.visit.repository.ServiceVisitRepository;
import com.sochi.fieldservice.incident.entity.RootCause;
import com.sochi.fieldservice.incident.dto.UpdateIncidentStatusRequest;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.incident.repository.IncidentHistoryRepository;
import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import com.sochi.fieldservice.engineer.entity.Engineer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;

import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private EngineerService engineerService;

    @Mock
    private IncidentHistoryRepository incidentHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IncidentCommentRepository incidentCommentRepository;

    @Mock
    private ServiceVisitRepository serviceVisitRepository;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void createIncidentShouldSaveIncident() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Router failure");
        request.setDescription("Network is unavailable");
        request.setPriority(IncidentPriority.HIGH);
        request.setLocation("Bhopal Office");

        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = incidentService.createIncident(request);

        assertNotNull(result);
        assertEquals("Router failure", result.getTitle());
        assertEquals(IncidentPriority.HIGH, result.getPriority());
        assertEquals("Bhopal Office", result.getLocation());
        assertNotNull(result.getSlaDeadline());

        verify(incidentRepository, times(1)).save(any(Incident.class));
        verify(incidentHistoryRepository, times(1)).save(any(IncidentHistory.class));
    }

    @Test
    void createIncidentShouldHandleNullPriority() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Router failure");
        request.setDescription("Network is unavailable");
        request.setLocation("Bhopal Office");

        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = incidentService.createIncident(request);

        assertNotNull(result);
        assertNull(result.getPriority());
        assertNull(result.getSlaDeadline());

        verify(incidentRepository, times(1)).save(any(Incident.class));
        verify(incidentHistoryRepository, times(1)).save(any(IncidentHistory.class));
    }

    @Test
    void getIncidentByIdShouldThrow404WhenIncidentDoesNotExist() {
        Long incidentId = 99999L;

        when(incidentRepository.findById(incidentId))
                .thenReturn(java.util.Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.getIncidentById(incidentId)
        );

        assertEquals(404, exception.getStatusCode().value());

        verify(incidentRepository).findById(incidentId);
    }

    @Test
    void updateIncidentStatusShouldUpdateAndSaveIncident() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setAssignedEngineer(new Engineer());

        when(incidentRepository.findById(1L))
                .thenReturn(java.util.Optional.of(incident));

        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = incidentService.updateIncidentStatus(
                1L, IncidentStatus.IN_PROGRESS
        );

        assertEquals(IncidentStatus.IN_PROGRESS, result.getStatus());

        verify(incidentRepository).findById(1L);
        verify(incidentRepository).save(incident);
        verify(incidentHistoryRepository).save(any(IncidentHistory.class));
    }

    @Test
    void updateIncidentStatusShouldThrow400WhenTransitionIsInvalid() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setStatus(IncidentStatus.OPEN);

        when(incidentRepository.findById(1L))
                .thenReturn(java.util.Optional.of(incident));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(1L, IncidentStatus.RESOLVED)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Invalid status transition"));
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void updateIncidentStatusShouldSetResolvedAtWhenMovingToResolved() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setAssignedEngineer(new Engineer());

        when(incidentRepository.findById(1L)).thenReturn(java.util.Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = incidentService.updateIncidentStatus(
                1L,
                IncidentStatus.RESOLVED,
                "Replaced faulty SFP transponder module",
                RootCause.HARDWARE_FAILURE
        );

        assertEquals(IncidentStatus.RESOLVED, result.getStatus());
        assertNotNull(result.getResolvedAt());
        assertNull(result.getClosedAt());
        assertEquals("Replaced faulty SFP transponder module", result.getResolutionNotes());
        assertEquals(RootCause.HARDWARE_FAILURE, result.getRootCause());
    }

    @Test
    void updateIncidentStatusToResolvedWithoutResolutionNotesShouldThrow400() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setAssignedEngineer(new Engineer());

        when(incidentRepository.findById(1L)).thenReturn(java.util.Optional.of(incident));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(1L, IncidentStatus.RESOLVED));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Resolution notes of at least 10 characters are required"));
    }

    @Test
    void updateIncidentStatusToResolvedWithShortNotesShouldThrow400() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setAssignedEngineer(new Engineer());

        when(incidentRepository.findById(1L)).thenReturn(java.util.Optional.of(incident));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(1L, IncidentStatus.RESOLVED, "Too short", null));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Resolution notes of at least 10 characters are required"));
    }

    @Test
    void updateIncidentStatusWithRequestDtoShouldResolveSuccessfully() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setAssignedEngineer(new Engineer());

        when(incidentRepository.findById(1L)).thenReturn(java.util.Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest();
        request.setStatus(IncidentStatus.RESOLVED);
        request.setResolutionNotes("Configured BGP peering session correctly");
        request.setRootCause(RootCause.CONFIGURATION_ERROR);

        Incident result = incidentService.updateIncidentStatus(1L, request, null);

        assertEquals(IncidentStatus.RESOLVED, result.getStatus());
        assertNotNull(result.getResolvedAt());
        assertEquals("Configured BGP peering session correctly", result.getResolutionNotes());
        assertEquals(RootCause.CONFIGURATION_ERROR, result.getRootCause());
    }

    @Test
    void updateIncidentStatusShouldSetClosedAtWhenMovingToClosed() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setStatus(IncidentStatus.RESOLVED);

        when(incidentRepository.findById(1L)).thenReturn(java.util.Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = incidentService.updateIncidentStatus(1L, IncidentStatus.CLOSED);

        assertEquals(IncidentStatus.CLOSED, result.getStatus());
        assertNotNull(result.getClosedAt());
    }

    @Test
    void updateIncidentStatusShouldClearTimestampsWhenReopeningTicket() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setAssignedEngineer(new Engineer());
        assertNotNull(incident.getResolvedAt());

        when(incidentRepository.findById(1L)).thenReturn(java.util.Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = incidentService.updateIncidentStatus(1L, IncidentStatus.IN_PROGRESS);

        assertEquals(IncidentStatus.IN_PROGRESS, result.getStatus());
        assertNull(result.getResolvedAt());
        assertNull(result.getClosedAt());
    }

    @Test
    void assignIncidentShouldAssignEngineerAndUpdateStatus() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setStatus(IncidentStatus.OPEN);

        Engineer engineer = new Engineer();
        engineer.setName("Test Engineer");
        engineer.setEmail("test@example.com");
        engineer.setSpecialization("Network Support");

        when(incidentRepository.findById(1L))
                .thenReturn(java.util.Optional.of(incident));

        when(engineerService.getEngineerByIdForUpdate(2L))
                .thenReturn(engineer);

        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = incidentService.assignIncident(1L, 2L);

        assertEquals(engineer, result.getAssignedEngineer());
        assertEquals(IncidentStatus.ASSIGNED, result.getStatus());

        verify(incidentRepository).findById(1L);
        verify(engineerService).getEngineerByIdForUpdate(2L);
        verify(incidentRepository).save(incident);
        verify(incidentHistoryRepository).save(any(IncidentHistory.class));
    }

    @Test
    void assignIncidentShouldThrow400WhenEngineerIsInactive() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setStatus(IncidentStatus.OPEN);

        Engineer engineer = new Engineer();
        engineer.setName("Inactive Tech");
        engineer.setActive(false);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(engineerService.getEngineerByIdForUpdate(2L)).thenReturn(engineer);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.assignIncident(1L, 2L));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("inactive"));
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void assignIncidentShouldThrow400WhenEngineerIsOffDuty() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setStatus(IncidentStatus.OPEN);

        Engineer engineer = new Engineer();
        engineer.setName("OffDuty Tech");
        engineer.setActive(true);
        engineer.setAvailabilityStatus(AvailabilityStatus.OFF_DUTY);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(engineerService.getEngineerByIdForUpdate(2L)).thenReturn(engineer);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.assignIncident(1L, 2L));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("off-duty"));
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void assignIncidentShouldThrow400WhenIncidentIsClosed() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setStatus(IncidentStatus.CLOSED);

        when(incidentRepository.findById(1L))
                .thenReturn(java.util.Optional.of(incident));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.assignIncident(1L, 2L)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Cannot assign engineer to a closed incident", exception.getReason());
        verify(incidentRepository, never()).save(any(Incident.class));
        verify(engineerService, never()).getEngineerByIdForUpdate(anyLong());
    }

    @Test
    void assignIncidentShouldThrow409WhenEngineerReachedWorkloadCapacity() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setStatus(IncidentStatus.OPEN);

        Engineer engineer = new Engineer();
        engineer.setName("Test Engineer");

        when(incidentRepository.findById(1L))
                .thenReturn(java.util.Optional.of(incident));
        when(engineerService.getEngineerByIdForUpdate(2L))
                .thenReturn(engineer);
        when(incidentRepository.countByAssignedEngineerIdAndStatusIn(eq(2L), anyList()))
                .thenReturn(3L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.assignIncident(1L, 2L)
        );

        assertEquals(409, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("maximum active workload capacity"));
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void assignIncidentShouldAllowReassignmentToSameEngineer() {
        Engineer engineer = new Engineer();
        engineer.setName("Test Engineer");
        engineer.setActive(true);
        engineer.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        ReflectionTestUtils.setField(engineer, "id", 2L);

        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignedEngineer(engineer);

        when(incidentRepository.findById(1L))
                .thenReturn(java.util.Optional.of(incident));
        when(engineerService.getEngineerByIdForUpdate(2L))
                .thenReturn(engineer);
        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = incidentService.assignIncident(1L, 2L);

        assertNotNull(result);
        assertEquals(engineer, result.getAssignedEngineer());
        verify(incidentRepository, never()).countByAssignedEngineerIdAndStatusIn(anyLong(), anyList());
        verify(incidentRepository).save(incident);
        verify(incidentHistoryRepository).save(any(IncidentHistory.class));
    }

    @Test
    void assignIncidentShouldAllowReassignmentToDifferentEngineerWhenCapacityAvailable() {
        Engineer oldEngineer = new Engineer();
        oldEngineer.setName("Old Engineer");
        ReflectionTestUtils.setField(oldEngineer, "id", 5L);

        Engineer newEngineer = new Engineer();
        newEngineer.setName("New Engineer");
        newEngineer.setActive(true);
        newEngineer.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        ReflectionTestUtils.setField(newEngineer, "id", 2L);

        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignedEngineer(oldEngineer);

        when(incidentRepository.findById(1L))
                .thenReturn(java.util.Optional.of(incident));
        when(engineerService.getEngineerByIdForUpdate(2L))
                .thenReturn(newEngineer);
        when(incidentRepository.countByAssignedEngineerIdAndStatusIn(eq(2L), anyList()))
                .thenReturn(1L);
        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = incidentService.assignIncident(1L, 2L);

        assertNotNull(result);
        assertEquals(newEngineer, result.getAssignedEngineer());
        verify(incidentRepository).countByAssignedEngineerIdAndStatusIn(eq(2L), anyList());
        verify(incidentRepository).save(incident);
        verify(incidentHistoryRepository).save(any(IncidentHistory.class));
    }

    @Test
    void getIncidentsShouldFilterByStatus() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setStatus(IncidentStatus.OPEN);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Incident> expectedPage =
                new PageImpl<>(List.of(incident), pageable, 1);

        when(incidentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        Page<Incident> result = incidentService.getIncidents(
                IncidentStatus.OPEN, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(IncidentStatus.OPEN,
                result.getContent().get(0).getStatus());

        verify(incidentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getIncidentsShouldFilterByPriority() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setPriority(IncidentPriority.CRITICAL);
        incident.setStatus(IncidentStatus.OPEN);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Incident> expectedPage =
                new PageImpl<>(List.of(incident), pageable, 1);

        when(incidentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        Page<Incident> result = incidentService.getIncidents(
                null, IncidentPriority.CRITICAL, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(IncidentPriority.CRITICAL,
                result.getContent().get(0).getPriority());

        verify(incidentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getIncidentsShouldFilterByStatusAndPriority() {
        Incident incident = new Incident();
        incident.setTitle("Core Switch Down");
        incident.setPriority(IncidentPriority.CRITICAL);
        incident.setStatus(IncidentStatus.OPEN);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Incident> expectedPage =
                new PageImpl<>(List.of(incident), pageable, 1);

        when(incidentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        Page<Incident> result = incidentService.getIncidents(
                IncidentStatus.OPEN, IncidentPriority.CRITICAL, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(IncidentStatus.OPEN,
                result.getContent().get(0).getStatus());
        assertEquals(IncidentPriority.CRITICAL,
                result.getContent().get(0).getPriority());

        verify(incidentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getIncidentsShouldReturnAllWhenNoFiltersProvided() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setStatus(IncidentStatus.OPEN);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Incident> expectedPage =
                new PageImpl<>(List.of(incident), pageable, 1);

        when(incidentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        Page<Incident> result = incidentService.getIncidents(
                null, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Router failure",
                result.getContent().get(0).getTitle());

        verify(incidentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAllIncidentsShouldReturnListOfIncidents() {
        Incident incident1 = new Incident();
        incident1.setTitle("Router failure");
        incident1.setPriority(IncidentPriority.HIGH);
        incident1.setStatus(IncidentStatus.OPEN);

        Incident incident2 = new Incident();
        incident2.setTitle("Fiber cut");
        incident2.setPriority(IncidentPriority.CRITICAL);
        incident2.setStatus(IncidentStatus.OPEN);

        List<Incident> expectedIncidents = List.of(incident1, incident2);

        when(incidentRepository.findAll()).thenReturn(expectedIncidents);

        List<Incident> result = incidentService.getAllIncidents();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Router failure", result.get(0).getTitle());
        assertEquals("Fiber cut", result.get(1).getTitle());

        verify(incidentRepository, times(1)).findAll();
    }

    @Test
    void getIncidentHistoryShouldReturnHistoryList() {
        Incident incident = new Incident();
        incident.setTitle("Router failure");

        IncidentHistory history1 = new IncidentHistory(incident, "CREATED", "Incident created with status OPEN");
        IncidentHistory history2 = new IncidentHistory(incident, "STATUS_CHANGED", "Status changed from OPEN to IN_PROGRESS");
        List<IncidentHistory> expectedList = List.of(history1, history2);

        when(incidentRepository.findById(1L)).thenReturn(java.util.Optional.of(incident));
        when(incidentHistoryRepository.findByIncidentIdOrderByTimestampAsc(1L)).thenReturn(expectedList);

        List<IncidentHistory> result = incidentService.getIncidentHistory(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("CREATED", result.get(0).getAction());
        assertEquals("STATUS_CHANGED", result.get(1).getAction());

        verify(incidentRepository).findById(1L);
        verify(incidentHistoryRepository).findByIncidentIdOrderByTimestampAsc(1L);
    }

    @Test
    void getIncidentHistoryShouldThrow404WhenIncidentDoesNotExist() {
        when(incidentRepository.findById(99999L)).thenReturn(java.util.Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.getIncidentHistory(99999L)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(incidentRepository).findById(99999L);
        verifyNoInteractions(incidentHistoryRepository);
    }

    @Test
    void getIncidentStatsShouldReturnCalculatedStatistics() {
        when(incidentRepository.count()).thenReturn(10L);
        when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(3L);
        when(incidentRepository.countByStatus(IncidentStatus.ASSIGNED)).thenReturn(2L);
        when(incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS)).thenReturn(1L);
        when(incidentRepository.countByStatus(IncidentStatus.RESOLVED)).thenReturn(2L);
        when(incidentRepository.countByStatus(IncidentStatus.CLOSED)).thenReturn(2L);
        when(incidentRepository.countByStatusInAndSlaDeadlineBefore(anyList(), any(java.time.LocalDateTime.class)))
                .thenReturn(1L);

        IncidentStatsResponse stats = incidentService.getIncidentStats();

        assertNotNull(stats);
        assertEquals(10L, stats.getTotalIncidents());
        assertEquals(3L, stats.getOpenIncidents());
        assertEquals(2L, stats.getAssignedIncidents());
        assertEquals(1L, stats.getInProgressIncidents());
        assertEquals(2L, stats.getResolvedIncidents());
        assertEquals(2L, stats.getClosedIncidents());
        assertEquals(6L, stats.getActiveIncidents());
        assertEquals(1L, stats.getSlaBreachedIncidents());

        verify(incidentRepository).count();
        verify(incidentRepository).countByStatus(IncidentStatus.OPEN);
        verify(incidentRepository).countByStatus(IncidentStatus.ASSIGNED);
        verify(incidentRepository).countByStatus(IncidentStatus.IN_PROGRESS);
        verify(incidentRepository).countByStatus(IncidentStatus.RESOLVED);
        verify(incidentRepository).countByStatus(IncidentStatus.CLOSED);
        verify(incidentRepository).countByStatusInAndSlaDeadlineBefore(anyList(), any(java.time.LocalDateTime.class));
    }

    @Test
    void getIncidentsForEngineerShouldReturnOnlyAssignedIncidents() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 42L);
        User techUser = new User("tech", "tech@test.com", "pass", Role.ROLE_ENGINEER, engineer);

        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(techUser));
        when(incidentRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(new Incident())));

        Page<Incident> result = incidentService.getIncidents(null, null, PageRequest.of(0, 10), "tech");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(incidentRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 10)));
    }

    @Test
    void getIncidentsForEngineerWithStatusAndPriorityShouldFilterCorrectly() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 42L);
        User techUser = new User("tech", "tech@test.com", "pass", Role.ROLE_ENGINEER, engineer);

        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(techUser));
        when(incidentRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(new Incident())));

        Page<Incident> result = incidentService.getIncidents(IncidentStatus.ASSIGNED, IncidentPriority.HIGH, PageRequest.of(0, 10), "tech");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(incidentRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 10)));
    }

    @Test
    void getIncidentsForEngineerWithStatusOnlyShouldFilterByStatus() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 42L);
        User techUser = new User("tech", "tech@test.com", "pass", Role.ROLE_ENGINEER, engineer);

        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(techUser));
        when(incidentRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(new Incident())));

        Page<Incident> result = incidentService.getIncidents(IncidentStatus.IN_PROGRESS, null, PageRequest.of(0, 10), "tech");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getIncidentsForEngineerWithPriorityOnlyShouldFilterByPriority() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 42L);
        User techUser = new User("tech", "tech@test.com", "pass", Role.ROLE_ENGINEER, engineer);

        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(techUser));
        when(incidentRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(new Incident())));

        Page<Incident> result = incidentService.getIncidents(null, IncidentPriority.CRITICAL, PageRequest.of(0, 10), "tech");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getIncidentsForUnlinkedEngineerShouldReturnEmptyPage() {
        User unlinkedTech = new User("tech2", "tech2@test.com", "pass", Role.ROLE_ENGINEER, null);
        when(userRepository.findByUsername("tech2")).thenReturn(Optional.of(unlinkedTech));

        Page<Incident> result = incidentService.getIncidents(null, null, PageRequest.of(0, 10), "tech2");

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verifyNoInteractions(incidentRepository);
    }

    @Test
    void getMyIncidentsShouldReturnUserAssignedIncidents() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 42L);
        User techUser = new User("tech", "tech@test.com", "pass", Role.ROLE_ENGINEER, engineer);

        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(techUser));
        when(incidentRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(new Incident())));

        Page<Incident> result = incidentService.getMyIncidents(null, null, PageRequest.of(0, 10), "tech");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getMyIncidentsWithNullUsernameShouldThrow401() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.getMyIncidents(null, null, PageRequest.of(0, 10), null));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void getMyIncidentsWithUserNotFoundShouldThrow404() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.getMyIncidents(null, null, PageRequest.of(0, 10), "nonexistent"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getMyIncidentsForUnlinkedUserShouldReturnEmptyPage() {
        User unlinked = new User("user_no_eng", "email@test.com", "pass", Role.ROLE_ENGINEER, null);
        when(userRepository.findByUsername("user_no_eng")).thenReturn(Optional.of(unlinked));

        Page<Incident> result = incidentService.getMyIncidents(null, null, PageRequest.of(0, 10), "user_no_eng");
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void updateIncidentStatusAsEngineerOnOwnIncidentShouldSucceed() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 10L);
        User tech = new User("tech1", "tech1@test.com", "pass", Role.ROLE_ENGINEER, engineer);

        Incident incident = new Incident();
        incident.setId(1L);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignedEngineer(engineer);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("tech1")).thenReturn(Optional.of(tech));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        Incident updated = incidentService.updateIncidentStatus(1L, IncidentStatus.IN_PROGRESS, "tech1");

        assertNotNull(updated);
        assertEquals(IncidentStatus.IN_PROGRESS, updated.getStatus());
        verify(incidentHistoryRepository).save(any(IncidentHistory.class));
    }

    @Test
    void updateIncidentStatusAsEngineerOnOtherEngineerIncidentShouldThrow403() {
        Engineer engineer1 = new Engineer();
        ReflectionTestUtils.setField(engineer1, "id", 10L);
        Engineer engineer2 = new Engineer();
        ReflectionTestUtils.setField(engineer2, "id", 20L);

        User tech1 = new User("tech1", "tech1@test.com", "pass", Role.ROLE_ENGINEER, engineer1);

        Incident incident = new Incident();
        incident.setId(1L);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignedEngineer(engineer2);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("tech1")).thenReturn(Optional.of(tech1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(1L, IncidentStatus.IN_PROGRESS, "tech1"));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Engineers can only update status on incidents assigned to them"));
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void updateIncidentStatusAsEngineerOnUnassignedIncidentShouldThrow403() {
        Engineer engineer1 = new Engineer();
        ReflectionTestUtils.setField(engineer1, "id", 10L);
        User tech1 = new User("tech1", "tech1@test.com", "pass", Role.ROLE_ENGINEER, engineer1);

        Incident incident = new Incident();
        incident.setId(1L);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setAssignedEngineer(null);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("tech1")).thenReturn(Optional.of(tech1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(1L, IncidentStatus.IN_PROGRESS, "tech1"));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Engineers can only update status on incidents assigned to them"));
    }

    @Test
    void updateIncidentStatusAsAdminCanUpdateAnyIncident() {
        Engineer engineer2 = new Engineer();
        ReflectionTestUtils.setField(engineer2, "id", 20L);

        User admin = new User("admin", "admin@test.com", "pass", Role.ROLE_ADMIN, null);

        Incident incident = new Incident();
        incident.setId(1L);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignedEngineer(engineer2);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        Incident updated = incidentService.updateIncidentStatus(1L, IncidentStatus.IN_PROGRESS, "admin");

        assertNotNull(updated);
        assertEquals(IncidentStatus.IN_PROGRESS, updated.getStatus());
    }

    @Test
    void assignIncidentWhenInProgressShouldRetainInProgressStatus() {
        Engineer engineer = new Engineer();
        engineer.setName("Jane Tech");

        Incident incident = new Incident();
        incident.setTitle("Fibre loop cut");
        incident.setStatus(IncidentStatus.IN_PROGRESS);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(engineerService.getEngineerByIdForUpdate(2L)).thenReturn(engineer);
        when(incidentRepository.countByAssignedEngineerIdAndStatusIn(eq(2L), anyList())).thenReturn(0L);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        Incident result = incidentService.assignIncident(1L, 2L);

        assertEquals(IncidentStatus.IN_PROGRESS, result.getStatus());
        assertEquals(engineer, result.getAssignedEngineer());
        verify(incidentRepository).save(incident);
    }

    @Test
    void assignIncidentWhenResolvedShouldRetainResolvedStatus() {
        Engineer engineer = new Engineer();
        engineer.setName("Bob Wire");

        Incident incident = new Incident();
        incident.setTitle("Power failure");
        incident.setStatus(IncidentStatus.RESOLVED);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(engineerService.getEngineerByIdForUpdate(3L)).thenReturn(engineer);
        when(incidentRepository.countByAssignedEngineerIdAndStatusIn(eq(3L), anyList())).thenReturn(1L);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        Incident result = incidentService.assignIncident(1L, 3L);

        assertEquals(IncidentStatus.RESOLVED, result.getStatus());
        assertEquals(engineer, result.getAssignedEngineer());
    }

    @Test
    void updateIncidentStatusToInProgressWithoutEngineerShouldThrow400() {
        Incident incident = new Incident();
        incident.setId(1L);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setAssignedEngineer(null);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(1L, IncidentStatus.IN_PROGRESS));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Cannot transition to IN_PROGRESS without an assigned engineer"));
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void updateIncidentStatusToResolvedWithoutEngineerShouldThrow400() {
        Incident incident = new Incident();
        incident.setId(1L);
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setAssignedEngineer(null);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(1L, IncidentStatus.RESOLVED));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Cannot transition to RESOLVED without an assigned engineer"));
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void unassignIncidentWhenAssignedShouldResetToOpen() {
        Engineer engineer = new Engineer();
        engineer.setName("Alice Field");

        Incident incident = new Incident();
        incident.setId(10L);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignedEngineer(engineer);

        when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        Incident result = incidentService.unassignIncident(10L);

        assertEquals(IncidentStatus.OPEN, result.getStatus());
        assertNull(result.getAssignedEngineer());
        verify(incidentHistoryRepository).save(any(IncidentHistory.class));
    }

    @Test
    void unassignIncidentWhenInProgressShouldResetToOpen() {
        Engineer engineer = new Engineer();
        engineer.setName("Bob Tech");

        Incident incident = new Incident();
        incident.setId(11L);
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setAssignedEngineer(engineer);

        when(incidentRepository.findById(11L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        Incident result = incidentService.unassignIncident(11L);

        assertEquals(IncidentStatus.OPEN, result.getStatus());
        assertNull(result.getAssignedEngineer());
        verify(incidentHistoryRepository).save(any(IncidentHistory.class));
    }

    @Test
    void unassignIncidentWhenClosedShouldThrow400() {
        Incident incident = new Incident();
        incident.setId(12L);
        incident.setStatus(IncidentStatus.CLOSED);

        when(incidentRepository.findById(12L)).thenReturn(Optional.of(incident));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.unassignIncident(12L));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Cannot unassign engineer from a CLOSED incident"));
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void unassignIncidentWhenResolvedShouldThrow400() {
        Incident incident = new Incident();
        incident.setId(13L);
        incident.setStatus(IncidentStatus.RESOLVED);

        when(incidentRepository.findById(13L)).thenReturn(Optional.of(incident));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.unassignIncident(13L));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Cannot unassign engineer from a RESOLVED incident"));
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void createIncidentWithUsernameShouldRecordPerformedByInHistory() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Optical Splice Box Damaged");
        request.setDescription("Water ingress detected");
        request.setPriority(IncidentPriority.HIGH);

        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        Incident created = incidentService.createIncident(request, "dispatcher_joe");

        assertNotNull(created);
        verify(incidentHistoryRepository).save(argThat(h ->
                "CREATED".equals(h.getAction()) && "dispatcher_joe".equals(h.getPerformedBy())
        ));
    }

    @Test
    void assignIncidentWithUsernameShouldRecordPerformedByInHistory() {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.OPEN);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        Engineer engineer = new Engineer();
        engineer.setName("Bob Tech");
        engineer.setActive(true);
        engineer.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        when(engineerService.getEngineerByIdForUpdate(2L)).thenReturn(engineer);
        when(incidentRepository.countByAssignedEngineerIdAndStatusIn(eq(2L), any())).thenReturn(0L);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        Incident result = incidentService.assignIncident(1L, 2L, "manager_mary");

        assertEquals(engineer, result.getAssignedEngineer());
        verify(incidentHistoryRepository).save(argThat(h ->
                "ASSIGNED".equals(h.getAction()) && "manager_mary".equals(h.getPerformedBy())
        ));
    }

    @Test
    void unassignIncidentWithUsernameShouldRecordPerformedByInHistory() {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.ASSIGNED);
        Engineer eng = new Engineer();
        eng.setName("Carol Danvers");
        incident.setAssignedEngineer(eng);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        Incident result = incidentService.unassignIncident(1L, "admin_jane");

        assertNull(result.getAssignedEngineer());
        assertEquals(IncidentStatus.OPEN, result.getStatus());
        verify(incidentHistoryRepository).save(argThat(h ->
                "UNASSIGNED".equals(h.getAction()) && "admin_jane".equals(h.getPerformedBy())
        ));
    }

    @Test
    void addCommentAsManagerShouldSucceed() {
        Incident incident = new Incident();
        incident.setStatus(IncidentStatus.ASSIGNED);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        User manager = new User("manager1", "manager@test.com", "pass", Role.ROLE_MANAGER);
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));

        when(incidentCommentRepository.save(any(IncidentComment.class))).thenAnswer(i -> {
            IncidentComment c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 100L);
            ReflectionTestUtils.setField(c, "createdAt", java.time.LocalDateTime.now());
            return c;
        });

        CreateIncidentCommentRequest req = new CreateIncidentCommentRequest("Dispatch priority escalated by NOC");
        IncidentCommentResponse response = incidentService.addComment(1L, req, "manager1");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("manager1", response.getAuthor());
        assertEquals("Dispatch priority escalated by NOC", response.getText());

        verify(incidentHistoryRepository).save(argThat(h ->
                "COMMENT_ADDED".equals(h.getAction()) && "manager1".equals(h.getPerformedBy())
        ));
    }

    @Test
    void addCommentAsAssignedEngineerShouldSucceed() {
        Engineer eng = new Engineer();
        ReflectionTestUtils.setField(eng, "id", 5L);

        Incident incident = new Incident();
        incident.setAssignedEngineer(eng);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        User tech = new User("tech5", "tech5@test.com", "pass", Role.ROLE_ENGINEER, eng);
        when(userRepository.findByUsername("tech5")).thenReturn(Optional.of(tech));

        when(incidentCommentRepository.save(any(IncidentComment.class))).thenAnswer(i -> {
            IncidentComment c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 105L);
            ReflectionTestUtils.setField(c, "createdAt", java.time.LocalDateTime.now());
            return c;
        });

        CreateIncidentCommentRequest req = new CreateIncidentCommentRequest("Arrived on site; inspecting patch panel");
        IncidentCommentResponse response = incidentService.addComment(1L, req, "tech5");

        assertNotNull(response);
        assertEquals(105L, response.getId());
        assertEquals("tech5", response.getAuthor());
    }

    @Test
    void addCommentAsViewerShouldThrow403() {
        Incident incident = new Incident();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        User viewer = new User("viewer1", "v@test.com", "pass", Role.ROLE_VIEWER);
        when(userRepository.findByUsername("viewer1")).thenReturn(Optional.of(viewer));

        CreateIncidentCommentRequest req = new CreateIncidentCommentRequest("Illegal comment by viewer");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.addComment(1L, req, "viewer1"));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Viewers have read-only access"));
        verify(incidentCommentRepository, never()).save(any(IncidentComment.class));
    }

    @Test
    void addCommentAsUnassignedEngineerShouldThrow403() {
        Engineer eng1 = new Engineer();
        ReflectionTestUtils.setField(eng1, "id", 1L);

        Engineer eng2 = new Engineer();
        ReflectionTestUtils.setField(eng2, "id", 2L);

        Incident incident = new Incident();
        incident.setAssignedEngineer(eng1);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        User peerTech = new User("peer_tech", "peer@test.com", "pass", Role.ROLE_ENGINEER, eng2);
        when(userRepository.findByUsername("peer_tech")).thenReturn(Optional.of(peerTech));

        CreateIncidentCommentRequest req = new CreateIncidentCommentRequest("Trying to comment on peer incident");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.addComment(1L, req, "peer_tech"));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Engineers can only comment on incidents assigned to them"));
        verify(incidentCommentRepository, never()).save(any(IncidentComment.class));
    }

    @Test
    void addCommentWithoutUsernameShouldThrow401() {
        Incident incident = new Incident();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        CreateIncidentCommentRequest req = new CreateIncidentCommentRequest("No auth");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.addComment(1L, req, null));

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void getCommentsShouldReturnCommentList() {
        when(incidentRepository.existsById(1L)).thenReturn(true);

        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);

        IncidentComment c1 = new IncidentComment(incident, "user1", "Comment 1");
        ReflectionTestUtils.setField(c1, "id", 1L);
        IncidentComment c2 = new IncidentComment(incident, "user2", "Comment 2");
        ReflectionTestUtils.setField(c2, "id", 2L);

        when(incidentCommentRepository.findByIncidentIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(c1, c2));

        List<IncidentCommentResponse> result = incidentService.getComments(1L);

        assertEquals(2, result.size());
        assertEquals("Comment 1", result.get(0).getText());
        assertEquals("Comment 2", result.get(1).getText());
    }

    @Test
    void getTimelineShouldReturnMergedSortedTimeline() {
        when(incidentRepository.existsById(1L)).thenReturn(true);

        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);

        java.time.LocalDateTime t1 = java.time.LocalDateTime.of(2026, 10, 29, 8, 0);
        java.time.LocalDateTime t2 = java.time.LocalDateTime.of(2026, 10, 29, 9, 0);
        java.time.LocalDateTime t3 = java.time.LocalDateTime.of(2026, 10, 29, 10, 0);

        IncidentHistory h = new IncidentHistory(incident, "CREATED", "Ticket opened", "dispatcher");
        ReflectionTestUtils.setField(h, "id", 1L);
        h.setTimestamp(t1);

        IncidentComment c = new IncidentComment(incident, "tech_lead", "Investigating link drop", t2);
        ReflectionTestUtils.setField(c, "id", 1L);

        Engineer eng = new Engineer();
        eng.setName("Field Eng");
        ServiceVisit v = new ServiceVisit(incident, eng, java.time.LocalDate.now(), "Cleaned fibre optic tips");
        ReflectionTestUtils.setField(v, "id", 1L);
        v.setCheckInAt(t3);

        when(incidentHistoryRepository.findByIncidentIdOrderByTimestampAsc(1L)).thenReturn(List.of(h));
        when(incidentCommentRepository.findByIncidentIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(c));
        when(serviceVisitRepository.findByIncidentIdOrderByVisitDateAscCheckInAtAscCreatedAtAsc(1L)).thenReturn(List.of(v));

        List<TimelineItemResponse> timeline = incidentService.getTimeline(1L);

        assertEquals(3, timeline.size());
        assertEquals("HISTORY", timeline.get(0).getItemType());
        assertEquals("COMMENT", timeline.get(1).getItemType());
        assertEquals("VISIT", timeline.get(2).getItemType());
        assertEquals(t1, timeline.get(0).getTimestamp());
        assertEquals(t2, timeline.get(1).getTimestamp());
        assertEquals(t3, timeline.get(2).getTimestamp());
    }

    @Test
    void updateIncidentShouldUpdateDetailsWithoutPriorityChange() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);
        incident.setTitle("Old Title");
        incident.setDescription("Old Desc");
        incident.setLocation("Old Loc");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setStatus(IncidentStatus.OPEN);
        java.time.LocalDateTime createdAt = java.time.LocalDateTime.now().minusHours(2);
        java.time.LocalDateTime oldDeadline = createdAt.plusHours(IncidentPriority.HIGH.getSlaHours());
        incident.setCreatedAt(createdAt);
        incident.setSlaDeadline(oldDeadline);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        UpdateIncidentRequest request = new UpdateIncidentRequest(
                "New Title",
                "New Desc",
                "New Loc",
                IncidentPriority.HIGH
        );

        Incident updated = incidentService.updateIncident(1L, request, "manager1");

        assertEquals("New Title", updated.getTitle());
        assertEquals("New Desc", updated.getDescription());
        assertEquals("New Loc", updated.getLocation());
        assertEquals(IncidentPriority.HIGH, updated.getPriority());
        assertEquals(oldDeadline, updated.getSlaDeadline());

        verify(incidentHistoryRepository).save(argThat(h ->
                h.getAction().equals("EDITED") &&
                h.getDescription().equals("Incident details updated") &&
                h.getPerformedBy().equals("manager1")
        ));
    }

    @Test
    void updateIncidentShouldUpdateDetailsWithPriorityChangeAndRecalculateSla() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);
        incident.setTitle("Router Down");
        incident.setDescription("Old Desc");
        incident.setLocation("Old Loc");
        incident.setPriority(IncidentPriority.LOW); // 48h
        incident.setStatus(IncidentStatus.OPEN);
        java.time.LocalDateTime createdAt = java.time.LocalDateTime.now().minusHours(1);
        incident.setCreatedAt(createdAt);
        incident.setSlaDeadline(createdAt.plusHours(IncidentPriority.LOW.getSlaHours()));

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        UpdateIncidentRequest request = new UpdateIncidentRequest(
                "Router Down - Critical",
                "Updated Desc",
                "Updated Loc",
                IncidentPriority.CRITICAL // 4h
        );

        Incident updated = incidentService.updateIncident(1L, request, "admin");

        assertEquals(IncidentPriority.CRITICAL, updated.getPriority());
        assertEquals(createdAt.plusHours(IncidentPriority.CRITICAL.getSlaHours()), updated.getSlaDeadline());

        verify(incidentHistoryRepository).save(argThat(h ->
                h.getAction().equals("EDITED") &&
                h.getDescription().contains("Priority changed from LOW to CRITICAL") &&
                h.getPerformedBy().equals("admin")
        ));
    }

    @Test
    void updateIncidentShouldThrowBadRequestWhenIncidentIsClosed() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);
        incident.setStatus(IncidentStatus.CLOSED);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        UpdateIncidentRequest request = new UpdateIncidentRequest("T", "D", "L", IncidentPriority.HIGH);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncident(1L, request, "admin"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("closed"));
    }

    @Test
    void updateIncidentShouldThrowForbiddenWhenUserIsNotAdminOrManager() {
        User engineerUser = new User("tech1", "tech@test.com", "pass", Role.ROLE_ENGINEER, null);
        when(userRepository.findByUsername("tech1")).thenReturn(Optional.of(engineerUser));

        UpdateIncidentRequest request = new UpdateIncidentRequest("T", "D", "L", IncidentPriority.HIGH);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncident(1L, request, "tech1"));
        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("administrators and managers"));
    }

    @Test
    void updateIncidentShouldThrowNotFoundWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateIncidentRequest request = new UpdateIncidentRequest("T", "D", "L", IncidentPriority.HIGH);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidentService.updateIncident(999L, request, "admin"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getIncidentsWithFilterCriteriaShouldQueryRepositoryWithSpecification() {
        IncidentFilterCriteria criteria = new IncidentFilterCriteria();
        criteria.setSearch("fiber");
        criteria.setBreachedOnly(true);
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Incident> expectedPage = new PageImpl<>(List.of(new Incident()));
        when(incidentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        Page<Incident> result = incidentService.getIncidents(criteria, pageable, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(incidentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getMyIncidentsWithFilterCriteriaShouldQueryRepositoryWithSpecification() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 15L);
        User tech = new User("tech15", "t15@test.com", "pass", Role.ROLE_ENGINEER, engineer);

        when(userRepository.findByUsername("tech15")).thenReturn(Optional.of(tech));

        IncidentFilterCriteria criteria = new IncidentFilterCriteria();
        criteria.setSearch("core");
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Incident> expectedPage = new PageImpl<>(List.of(new Incident()));
        when(incidentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        Page<Incident> result = incidentService.getMyIncidents(criteria, pageable, "tech15");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(incidentRepository).findAll(any(Specification.class), eq(pageable));
    }
}
