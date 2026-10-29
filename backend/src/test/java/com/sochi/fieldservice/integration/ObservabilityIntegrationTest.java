package com.sochi.fieldservice.integration;

import com.sochi.fieldservice.security.jwt.JwtUtils;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ObservabilityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("Verify /actuator/health is publicly accessible and contains custom IncidentSystem health details")
    void shouldExposeHealthIndicatorPublicly() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.incidentSystem.status").value("UP"))
                .andExpect(jsonPath("$.components.incidentSystem.details.systemStatus").value("OPERATIONAL"));
    }

    @Test
    @DisplayName("Verify /actuator/info is publicly accessible")
    void shouldExposeInfoPublicly() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verify /actuator/metrics enforces Admin authorization")
    void shouldEnforceAdminRoleOnMetricsEndpoint() throws Exception {
        User admin = userRepository.save(new User("obs_admin", "obs_admin@example.com", "pass", Role.ROLE_ADMIN));
        User manager = userRepository.save(new User("obs_mgr", "obs_mgr@example.com", "pass", Role.ROLE_MANAGER));

        String adminToken = "Bearer " + jwtUtils.generateToken(admin);
        String mgrToken = "Bearer " + jwtUtils.generateToken(manager);

        // 1. Unauthenticated -> 401
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());

        // 2. Non-admin -> 403
        mockMvc.perform(get("/actuator/metrics").header("Authorization", mgrToken))
                .andExpect(status().isForbidden());

        // 3. Admin -> 200 OK
        mockMvc.perform(get("/actuator/metrics").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    @DisplayName("Verify /actuator/prometheus exports Prometheus formatted metrics to Admin")
    void shouldExportPrometheusMetricsToAdmin() throws Exception {
        User admin = userRepository.save(new User("prom_admin", "prom_admin@example.com", "pass", Role.ROLE_ADMIN));
        String adminToken = "Bearer " + jwtUtils.generateToken(admin);

        // 1. Unauthenticated -> 401
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());

        // 2. Admin -> 200 OK with Prometheus exposition format
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("incident_active_gauge")));
    }
}
