package com.sochi.fieldservice.integration;

import com.sochi.fieldservice.security.AuthService;
import com.sochi.fieldservice.security.dto.AuthResponse;
import com.sochi.fieldservice.security.dto.LoginRequest;
import com.sochi.fieldservice.security.dto.RegisterRequest;
import com.sochi.fieldservice.security.jwt.JwtUtils;
import com.sochi.fieldservice.user.dto.AdminCreateUserRequest;
import com.sochi.fieldservice.user.dto.UserResponse;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import com.sochi.fieldservice.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("Verify public endpoints are accessible without authentication")
    void shouldAllowPublicEndpointsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/v1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/database-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true));
    }

    @Test
    @DisplayName("Verify protected endpoint returns 401 Unauthorized when token is missing")
    void shouldRejectProtectedEndpointWhenTokenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Verify protected endpoint returns 401 Unauthorized when token is malformed")
    void shouldRejectProtectedEndpointWhenTokenMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer invalid.malformed.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Verify registered user can access protected endpoints using Bearer JWT")
    void shouldAllowAccessWithValidJwtToken() throws Exception {
        AuthResponse auth = authService.register(
                new RegisterRequest("field_user", "field_user@example.com", "pass1234")
        );

        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + auth.getToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verify Role-Based Access Control: Engineer receives 403 Forbidden on Admin endpoint")
    void shouldEnforceRbacAndReturn403ForUnauthorizedRole() throws Exception {
        AuthResponse engineerAuth = authService.register(
                new RegisterRequest("field_eng", "field_eng@example.com", "pass1234")
        );

        mockMvc.perform(post("/api/v1/engineers")
                        .header("Authorization", "Bearer " + engineerAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "New Tech",
                                "email": "new.tech@example.com",
                                "specialization": "Fiber"
                            }
                            """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access Denied: You do not have permission to access this resource"));
    }

    @Test
    @DisplayName("Verify Role-Based Access Control: Admin can access Admin endpoints")
    void shouldAllowAdminToAccessAdminEndpoints() throws Exception {
        userService.createUser(
                new AdminCreateUserRequest("super_admin", "super_admin@example.com", "pass1234", Role.ROLE_ADMIN)
        );

        AuthResponse adminAuth = authService.login(new LoginRequest("super_admin", "pass1234"));

        mockMvc.perform(post("/api/v1/engineers")
                        .header("Authorization", "Bearer " + adminAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "New Tech",
                                "email": "new.tech@example.com",
                                "specialization": "Fiber"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Tech"));
    }

    @Test
    @DisplayName("Verify public register request containing role=ROLE_ADMIN still yields ROLE_ENGINEER")
    void shouldYieldRoleEngineerEvenWhenRoleAdminIsProvidedInPublicRegister() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "sneaky_admin",
                                "email": "sneaky@example.com",
                                "password": "password123",
                                "role": "ROLE_ADMIN"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("sneaky_admin"))
                .andExpect(jsonPath("$.role").value("ROLE_ENGINEER"));

        User savedUser = userRepository.findByUsername("sneaky_admin").orElseThrow();
        assertEquals(Role.ROLE_ENGINEER, savedUser.getRole());
    }

    @Test
    @DisplayName("Verify Admin can create users with any role via POST /api/v1/admin/users")
    void shouldAllowAdminToCreateUserWithAnyRole() throws Exception {
        userService.createUser(
                new AdminCreateUserRequest("sys_admin", "sys_admin@example.com", "pass1234", Role.ROLE_ADMIN)
        );
        AuthResponse adminAuth = authService.login(new LoginRequest("sys_admin", "pass1234"));

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "new_manager",
                                "email": "manager@example.com",
                                "password": "securepassword",
                                "role": "ROLE_MANAGER"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("new_manager"))
                .andExpect(jsonPath("$.email").value("manager@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_MANAGER"))
                .andExpect(jsonPath("$.enabled").value(true));

        User created = userRepository.findByUsername("new_manager").orElseThrow();
        assertEquals(Role.ROLE_MANAGER, created.getRole());
    }

    @Test
    @DisplayName("Verify Engineer receives 403 Forbidden when attempting POST /api/v1/admin/users")
    void shouldDenyEngineerFromAdminUsersEndpoint() throws Exception {
        AuthResponse engineerAuth = authService.register(
                new RegisterRequest("field_worker", "field_worker@example.com", "pass1234")
        );

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + engineerAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "escalated_user",
                                "email": "escalated@example.com",
                                "password": "securepassword",
                                "role": "ROLE_ADMIN"
                            }
                            """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Verify unauthenticated request to /api/v1/admin/users returns 401 Unauthorized")
    void shouldDenyUnauthenticatedFromAdminUsersEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "anon_user",
                                "email": "anon@example.com",
                                "password": "securepassword",
                                "role": "ROLE_ADMIN"
                            }
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
