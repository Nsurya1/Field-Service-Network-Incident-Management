package com.sochi.fieldservice.incident.controller;

import com.sochi.fieldservice.incident.IncidentService;
import com.sochi.fieldservice.incident.dto.AssignIncidentRequest;
import com.sochi.fieldservice.incident.dto.CreateIncidentRequest;
import com.sochi.fieldservice.incident.dto.IncidentStatsResponse;
import com.sochi.fieldservice.incident.dto.UpdateIncidentStatusRequest;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = IncidentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentService incidentService;

    @Test
    void shouldReturnEmptyIncidentPage() throws Exception {
        PageRequest pageable = PageRequest.of(
                0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Incident> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(incidentService.getIncidents(null, null, pageable)).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(incidentService).getIncidents(null, null, pageable);
    }

    @Test
    void shouldRejectNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }

    @Test
    void shouldRejectZeroPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }

    @Test
    void shouldRejectPageSizeAbove100() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }

    @Test
    void shouldRejectInvalidStatusParameter() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }

    @Test
    void shouldRejectInvalidPriorityParameter() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .param("priority", "NOT_A_PRIORITY"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }

    @Test
    void shouldGetIncidentById() throws Exception {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setDescription("Router is not responding");
        incident.setPriority(IncidentPriority.CRITICAL);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setLocation("Bhopal");

        when(incidentService.getIncidentById(1L)).thenReturn(incident);

        mockMvc.perform(get("/api/v1/incidents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Router failure"))
                .andExpect(jsonPath("$.description").value("Router is not responding"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.location").value("Bhopal"))
                .andExpect(jsonPath("$.assignedEngineerId").value((Object) null))
                .andExpect(jsonPath("$.assignedEngineerName").value((Object) null));

        verify(incidentService).getIncidentById(1L);
    }

    @Test
    void shouldReturn404WhenIncidentDoesNotExist() throws Exception {
        when(incidentService.getIncidentById(99999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        mockMvc.perform(get("/api/v1/incidents/99999"))
                .andExpect(status().isNotFound());

        verify(incidentService).getIncidentById(99999L);
    }

    @Test
    void shouldUpdateIncidentStatus() throws Exception {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setDescription("Router is not responding");
        incident.setPriority(IncidentPriority.CRITICAL);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setLocation("Bhopal");

        when(incidentService.updateIncidentStatus(1L, IncidentStatus.RESOLVED))
                .thenReturn(incident);

        mockMvc.perform(patch("/api/v1/incidents/1/status")
                        .contentType("application/json")
                        .content("""
                            {
                                "status": "RESOLVED"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Router failure"))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.location").value("Bhopal"));

        verify(incidentService).updateIncidentStatus(1L, IncidentStatus.RESOLVED);
    }

    @Test
    void shouldRejectStatusUpdateWhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/incidents/1/status")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }

    @Test
    void shouldAssignIncidentToEngineer() throws Exception {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setDescription("Router is not responding");
        incident.setPriority(IncidentPriority.CRITICAL);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setLocation("Bhopal");

        when(incidentService.assignIncident(1L, 5L)).thenReturn(incident);

        mockMvc.perform(patch("/api/v1/incidents/1/assign")
                        .contentType("application/json")
                        .content("""
                            {
                                "engineerId": 5
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Router failure"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.location").value("Bhopal"));

        verify(incidentService).assignIncident(1L, 5L);
    }

    @Test
    void shouldRejectAssignmentWhenEngineerIdIsZero() throws Exception {
        mockMvc.perform(patch("/api/v1/incidents/1/assign")
                        .contentType("application/json")
                        .content("""
                            {
                                "engineerId": 0
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }

    @Test
    void shouldRejectAssignmentWhenEngineerIdIsMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/incidents/1/assign")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }

    @Test
    void shouldCreateIncident() throws Exception {
        Incident incident = new Incident();
        incident.setTitle("Router failure");
        incident.setDescription("Router is not responding");
        incident.setPriority(IncidentPriority.CRITICAL);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setLocation("Bhopal");

        when(incidentService.createIncident(any(CreateIncidentRequest.class))).thenReturn(incident);

        mockMvc.perform(post("/api/v1/incidents")
                        .contentType("application/json")
                        .content("""
                            {
                                "title": "Router failure",
                                "description": "Router is not responding",
                                "priority": "CRITICAL",
                                "location": "Bhopal"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Router failure"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.location").value("Bhopal"));

        verify(incidentService).createIncident(any(CreateIncidentRequest.class));
    }

    @Test
    void shouldGetIncidentHistory() throws Exception {
        Incident incident = new Incident();
        incident.setTitle("Router failure");

        IncidentHistory history1 = new IncidentHistory(incident, "CREATED", "Incident created with status OPEN");
        IncidentHistory history2 = new IncidentHistory(incident, "STATUS_CHANGED", "Status changed from OPEN to IN_PROGRESS");

        when(incidentService.getIncidentHistory(1L)).thenReturn(List.of(history1, history2));

        mockMvc.perform(get("/api/v1/incidents/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[0].description").value("Incident created with status OPEN"))
                .andExpect(jsonPath("$[1].action").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$[1].description").value("Status changed from OPEN to IN_PROGRESS"));

        verify(incidentService).getIncidentHistory(1L);
    }

    @Test
    void shouldReturn404WhenGettingHistoryForNonExistentIncident() throws Exception {
        when(incidentService.getIncidentHistory(99999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found with ID: 99999"));

        mockMvc.perform(get("/api/v1/incidents/99999/history"))
                .andExpect(status().isNotFound());

        verify(incidentService).getIncidentHistory(99999L);
    }

    @Test
    void shouldGetIncidentStats() throws Exception {
        IncidentStatsResponse stats = new IncidentStatsResponse(
                10L, 3L, 2L, 1L, 2L, 2L, 6L, 1L
        );

        when(incidentService.getIncidentStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/incidents/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncidents").value(10))
                .andExpect(jsonPath("$.openIncidents").value(3))
                .andExpect(jsonPath("$.assignedIncidents").value(2))
                .andExpect(jsonPath("$.inProgressIncidents").value(1))
                .andExpect(jsonPath("$.resolvedIncidents").value(2))
                .andExpect(jsonPath("$.closedIncidents").value(2))
                .andExpect(jsonPath("$.activeIncidents").value(6))
                .andExpect(jsonPath("$.slaBreachedIncidents").value(1));

        verify(incidentService).getIncidentStats();
    }

    @Test
    void shouldGetMyIncidentsWhenAuthenticated() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Incident> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(incidentService.getMyIncidents(null, null, pageable, "tech_user")).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/incidents/mine")
                        .principal(() -> "tech_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(incidentService).getMyIncidents(null, null, pageable, "tech_user");
    }

    @Test
    void shouldRejectGetMyIncidentsWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectNegativePageOnGetMyIncidents() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/mine")
                        .principal(() -> "tech_user")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}
