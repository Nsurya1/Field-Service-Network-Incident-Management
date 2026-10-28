
package com.sochi.fieldservice.engineer.controller;

import tools.jackson.databind.ObjectMapper;
import com.sochi.fieldservice.engineer.dto.CreateEngineerRequest;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.service.EngineerService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.sochi.fieldservice.security.SecurityConfig;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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

    @Test
    void shouldCreateEngineer() throws Exception {
        CreateEngineerRequest request = new CreateEngineerRequest();
        request.setName("Rahul Sharma");
        request.setEmail("rahul@example.com");
        request.setSpecialization("Network Support");

        Engineer engineer = new Engineer();
        engineer.setName("Rahul Sharma");
        engineer.setEmail("rahul@example.com");
        engineer.setSpecialization("Network Support");

        when(engineerService.createEngineer(any(CreateEngineerRequest.class)))
                .thenReturn(engineer);

        mockMvc.perform(post("/api/v1/engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rahul Sharma"))
                .andExpect(jsonPath("$.email").value("rahul@example.com"));

        verify(engineerService).createEngineer(any(CreateEngineerRequest.class));
    }

    @Test
    void shouldGetAllEngineers() throws Exception {
        Engineer engineer = new Engineer();
        engineer.setName("Rahul Sharma");
        engineer.setEmail("rahul@example.com");
        engineer.setSpecialization("Network Support");

        when(engineerService.getAllEngineers())
                .thenReturn(List.of(engineer));

        mockMvc.perform(get("/api/v1/engineers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rahul Sharma"));

        verify(engineerService).getAllEngineers();
    }

    @Test
    void shouldGetEngineerById() throws Exception {
        Engineer engineer = new Engineer();
        engineer.setName("Rahul Sharma");
        engineer.setEmail("rahul@example.com");
        engineer.setSpecialization("Network Support");

        when(engineerService.getEngineerById(1L))
                .thenReturn(engineer);

        mockMvc.perform(get("/api/v1/engineers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rahul Sharma"));

        verify(engineerService).getEngineerById(1L);
    }

    @Test
    void shouldRejectInvalidEngineerRequest() throws Exception {
        CreateEngineerRequest request = new CreateEngineerRequest();
        request.setName("");
        request.setEmail("invalid-email");
        request.setSpecialization("");

        mockMvc.perform(post("/api/v1/engineers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(engineerService, never())
                .createEngineer(any(CreateEngineerRequest.class));
    }
}
