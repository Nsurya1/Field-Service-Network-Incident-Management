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

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private EngineerService engineerService;

    @Mock
    private IncidentHistoryRepository incidentHistoryRepository;

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

        when(engineerService.getEngineerById(2L))
                .thenReturn(engineer);

        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = incidentService.assignIncident(1L, 2L);

        assertEquals(engineer, result.getAssignedEngineer());
        assertEquals(IncidentStatus.ASSIGNED, result.getStatus());

        verify(incidentRepository).findById(1L);
        verify(engineerService).getEngineerById(2L);
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
        verify(engineerService, never()).getEngineerById(anyLong());
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
        when(engineerService.getEngineerById(2L))
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
        when(engineerService.getEngineerById(2L))
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
        when(engineerService.getEngineerById(2L))
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

}
