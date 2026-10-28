package com.sochi.fieldservice.engineer.controller;

import tools.jackson.databind.ObjectMapper;
import com.sochi.fieldservice.engineer.dto.CreateEngineerRequest;
import com.sochi.fieldservice.engineer.dto.EngineerResponse;
import com.sochi.fieldservice.engineer.dto.UpdateEngineerActiveRequest;
import com.sochi.fieldservice.engineer.dto.UpdateEngineerRequest;
import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.service.EngineerService;
import com.sochi.fieldservice.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = EngineerController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
class EngineerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EngineerService engineerService;

    private Engineer createSampleEngineer(Long id, String name, String email, String spec, String phone, boolean active, AvailabilityStatus status) {
        Engineer engineer = new Engineer();
        ReflectionTestUtils.setField(engineer, "id", id);
        engineer.setName(name);
        engineer.setEmail(email);
        engineer.setSpecialization(spec);
        engineer.setPhone(phone);
        engineer.setActive(active);
        engineer.setAvailabilityStatus(status);
        ReflectionTestUtils.setField(engineer, "createdAt", LocalDateTime.now());
        return engineer;
    }

    @Test
    @DisplayName("Verify POST /api/v1/engineers creates engineer and returns 201 Created with EngineerResponse")
    void shouldCreateEngineer() throws Exception {
        CreateEngineerRequest request = new CreateEngineerRequest("Rahul Sharma", "rahul@example.com", "Network Support", "+91-98765-43210");
        Engineer engineer = createSampleEngineer(1L, "Rahul Sharma", "rahul@example.com", "Network Support", "+91-98765-43210", true, AvailabilityStatus.AVAILABLE);

        when(engineerService.createEngineer(any(CreateEngineerRequest.class)))
                .thenReturn(new EngineerResponse(engineer, 0L));

        mockMvc.perform(post("/api/v1/engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Rahul Sharma"))
                .andExpect(jsonPath("$.email").value("rahul@example.com"))
                .andExpect(jsonPath("$.phone").value("+91-98765-43210"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.currentWorkload").value(0));

        verify(engineerService).createEngineer(any(CreateEngineerRequest.class));
    }

    @Test
    @DisplayName("Verify GET /api/v1/engineers returns 200 OK with list of EngineerResponse")
    void shouldGetAllEngineers() throws Exception {
        Engineer engineer = createSampleEngineer(1L, "Rahul Sharma", "rahul@example.com", "Network Support", null, true, AvailabilityStatus.AVAILABLE);

        when(engineerService.getAllEngineers())
                .thenReturn(List.of(new EngineerResponse(engineer, 2L)));

        mockMvc.perform(get("/api/v1/engineers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Rahul Sharma"))
                .andExpect(jsonPath("$[0].currentWorkload").value(2));

        verify(engineerService).getAllEngineers();
    }

    @Test
    @DisplayName("Verify GET /api/v1/engineers/{id} returns 200 OK with EngineerResponse")
    void shouldGetEngineerById() throws Exception {
        Engineer engineer = createSampleEngineer(1L, "Rahul Sharma", "rahul@example.com", "Network Support", "+91-11111", true, AvailabilityStatus.BUSY);

        when(engineerService.getEngineerResponseById(1L))
                .thenReturn(new EngineerResponse(engineer, 3L));

        mockMvc.perform(get("/api/v1/engineers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rahul Sharma"))
                .andExpect(jsonPath("$.availabilityStatus").value("BUSY"))
                .andExpect(jsonPath("$.currentWorkload").value(3));

        verify(engineerService).getEngineerResponseById(1L);
    }

    @Test
    @DisplayName("Verify PUT /api/v1/engineers/{id} updates engineer and returns 200 OK")
    void shouldUpdateEngineer() throws Exception {
        UpdateEngineerRequest request = new UpdateEngineerRequest("Rahul Updated", "+91-99999", "Core IP", AvailabilityStatus.BUSY);
        Engineer engineer = createSampleEngineer(1L, "Rahul Updated", "rahul@example.com", "Core IP", "+91-99999", true, AvailabilityStatus.BUSY);

        when(engineerService.updateEngineer(eq(1L), any(UpdateEngineerRequest.class)))
                .thenReturn(new EngineerResponse(engineer, 1L));

        mockMvc.perform(put("/api/v1/engineers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rahul Updated"))
                .andExpect(jsonPath("$.specialization").value("Core IP"))
                .andExpect(jsonPath("$.availabilityStatus").value("BUSY"));

        verify(engineerService).updateEngineer(eq(1L), any(UpdateEngineerRequest.class));
    }

    @Test
    @DisplayName("Verify PATCH /api/v1/engineers/{id}/active with JSON body updates active status")
    void shouldUpdateActiveStatusViaJsonBody() throws Exception {
        Engineer engineer = createSampleEngineer(1L, "Rahul", "rahul@example.com", "Fibre", null, false, AvailabilityStatus.AVAILABLE);

        when(engineerService.updateEngineerActiveStatus(1L, false))
                .thenReturn(new EngineerResponse(engineer, 0L));

        mockMvc.perform(patch("/api/v1/engineers/1/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateEngineerActiveRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(engineerService).updateEngineerActiveStatus(1L, false);
    }

    @Test
    @DisplayName("Verify PATCH /api/v1/engineers/{id}/active with query param updates active status")
    void shouldUpdateActiveStatusViaQueryParam() throws Exception {
        Engineer engineer = createSampleEngineer(1L, "Rahul", "rahul@example.com", "Fibre", null, true, AvailabilityStatus.AVAILABLE);

        when(engineerService.updateEngineerActiveStatus(1L, true))
                .thenReturn(new EngineerResponse(engineer, 0L));

        mockMvc.perform(patch("/api/v1/engineers/1/active").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        verify(engineerService).updateEngineerActiveStatus(1L, true);
    }

    @Test
    @DisplayName("Verify PATCH /api/v1/engineers/{id}/active without body or param returns 400 Bad Request")
    void shouldRejectPatchActiveWithoutFlag() throws Exception {
        mockMvc.perform(patch("/api/v1/engineers/1/active"))
                .andExpect(status().isBadRequest());

        verify(engineerService, never()).updateEngineerActiveStatus(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Verify POST /api/v1/engineers rejects invalid request payload")
    void shouldRejectInvalidEngineerRequest() throws Exception {
        CreateEngineerRequest request = new CreateEngineerRequest("", "invalid-email", "");

        mockMvc.perform(post("/api/v1/engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(engineerService, never()).createEngineer(any(CreateEngineerRequest.class));
    }

    @Test
    @DisplayName("Verify PUT /api/v1/engineers/{id} rejects blank name")
    void shouldRejectInvalidUpdateRequest() throws Exception {
        UpdateEngineerRequest request = new UpdateEngineerRequest("", "+91-12345", "Fibre", AvailabilityStatus.AVAILABLE);

        mockMvc.perform(put("/api/v1/engineers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(engineerService, never()).updateEngineer(anyLong(), any(UpdateEngineerRequest.class));
    }
}
