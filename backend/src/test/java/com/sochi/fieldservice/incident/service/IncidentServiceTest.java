package com.sochi.fieldservice.incident.service;

import com.sochi.fieldservice.engineer.service.EngineerService;

import com.sochi.fieldservice.incident.IncidentService;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
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

import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.incident.repository.IncidentHistoryRepository;
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

        Incident result = incidentService.updateIncidentStatus(1L, IncidentStatus.RESOLVED);

        assertEquals(IncidentStatus.RESOLVED, result.getStatus());
        assertNotNull(result.getResolvedAt());
        assertNull(result.getClosedAt());
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
        Engineer engineer = mock(Engineer.class);
        when(engineer.getId()).thenReturn(2L);

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
        Engineer oldEngineer = mock(Engineer.class);
        when(oldEngineer.getId()).thenReturn(5L);

        Engineer newEngineer = mock(Engineer.class);

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

        when(incidentRepository.findByStatus(
                IncidentStatus.OPEN, pageable))
                .thenReturn(expectedPage);

        Page<Incident> result = incidentService.getIncidents(
                IncidentStatus.OPEN, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(IncidentStatus.OPEN,
                result.getContent().get(0).getStatus());

        verify(incidentRepository).findByStatus(
                IncidentStatus.OPEN, pageable);
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

        when(incidentRepository.findByPriority(
                IncidentPriority.CRITICAL, pageable))
                .thenReturn(expectedPage);

        Page<Incident> result = incidentService.getIncidents(
                null, IncidentPriority.CRITICAL, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(IncidentPriority.CRITICAL,
                result.getContent().get(0).getPriority());

        verify(incidentRepository).findByPriority(
                IncidentPriority.CRITICAL, pageable);
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

        when(incidentRepository.findByStatusAndPriority(
                IncidentStatus.OPEN, IncidentPriority.CRITICAL, pageable))
                .thenReturn(expectedPage);

        Page<Incident> result = incidentService.getIncidents(
                IncidentStatus.OPEN, IncidentPriority.CRITICAL, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(IncidentStatus.OPEN,
                result.getContent().get(0).getStatus());
        assertEquals(IncidentPriority.CRITICAL,
                result.getContent().get(0).getPriority());

        verify(incidentRepository).findByStatusAndPriority(
                IncidentStatus.OPEN, IncidentPriority.CRITICAL, pageable);
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

        when(incidentRepository.findAll(pageable))
                .thenReturn(expectedPage);

        Page<Incident> result = incidentService.getIncidents(
                null, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Router failure",
                result.getContent().get(0).getTitle());

        verify(incidentRepository).findAll(pageable);
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
        when(incidentRepository.findByAssignedEngineerId(42L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(new Incident())));

        Page<Incident> result = incidentService.getIncidents(null, null, PageRequest.of(0, 10), "tech");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(incidentRepository).findByAssignedEngineerId(42L, PageRequest.of(0, 10));
    }

    @Test
    void getIncidentsForEngineerWithStatusAndPriorityShouldFilterCorrectly() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 42L);
        User techUser = new User("tech", "tech@test.com", "pass", Role.ROLE_ENGINEER, engineer);

        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(techUser));
        when(incidentRepository.findByAssignedEngineerIdAndStatusAndPriority(42L, IncidentStatus.ASSIGNED, IncidentPriority.HIGH, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(new Incident())));

        Page<Incident> result = incidentService.getIncidents(IncidentStatus.ASSIGNED, IncidentPriority.HIGH, PageRequest.of(0, 10), "tech");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(incidentRepository).findByAssignedEngineerIdAndStatusAndPriority(42L, IncidentStatus.ASSIGNED, IncidentPriority.HIGH, PageRequest.of(0, 10));
    }

    @Test
    void getIncidentsForEngineerWithStatusOnlyShouldFilterByStatus() {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", 42L);
        User techUser = new User("tech", "tech@test.com", "pass", Role.ROLE_ENGINEER, engineer);

        when(userRepository.findByUsername("tech")).thenReturn(Optional.of(techUser));
        when(incidentRepository.findByAssignedEngineerIdAndStatus(42L, IncidentStatus.IN_PROGRESS, PageRequest.of(0, 10)))
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
        when(incidentRepository.findByAssignedEngineerIdAndPriority(42L, IncidentPriority.CRITICAL, PageRequest.of(0, 10)))
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
        when(incidentRepository.findByAssignedEngineerId(42L, PageRequest.of(0, 10)))
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
}
