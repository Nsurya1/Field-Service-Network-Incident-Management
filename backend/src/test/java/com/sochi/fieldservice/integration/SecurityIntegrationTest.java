package com.sochi.fieldservice.integration;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    private EngineerRepository engineerRepository;

    @Autowired
    private IncidentRepository incidentRepository;

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
                                "name": "Unauthorized Add",
                                "email": "unauth@example.com",
                                "specialization": "Fibre"
                            }
                            """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access Denied: You do not have permission to access this resource"));
    }

    @Test
    @DisplayName("Verify Admin can access Admin-protected endpoints")
    void shouldAllowAdminToAccessAdminEndpoints() throws Exception {
        User admin = new User("super_admin", "admin@example.com", "hashed_pass", Role.ROLE_ADMIN);
        userRepository.save(admin);

        String adminToken = jwtUtils.generateToken(admin);

        mockMvc.perform(post("/api/v1/engineers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Valid Engineer",
                                "email": "valid.engineer@example.com",
                                "specialization": "Router"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Valid Engineer"))
                .andExpect(jsonPath("$.email").value("valid.engineer@example.com"));
    }

    @Test
    @DisplayName("Verify public register request ignoring any supplied role and creating ROLE_ENGINEER")
    void shouldIgnoreRoleFieldInPublicRegisterAndCreateEngineer() throws Exception {
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

    @Test
    @DisplayName("Verify ROLE_VIEWER has read-only access to incidents and stats, and receives 403 on mutations")
    void shouldAllowViewerReadAccessAndRejectMutations() throws Exception {
        userService.createUser(
                new AdminCreateUserRequest("viewer_user", "viewer@example.com", "pass1234", Role.ROLE_VIEWER)
        );
        AuthResponse viewerAuth = authService.login(new LoginRequest("viewer_user", "pass1234"));
        String token = "Bearer " + viewerAuth.getToken();

        // 1. GET /incidents -> 200 OK
        mockMvc.perform(get("/api/v1/incidents").header("Authorization", token))
                .andExpect(status().isOk());

        // 2. GET /incidents/stats -> 200 OK
        mockMvc.perform(get("/api/v1/incidents/stats").header("Authorization", token))
                .andExpect(status().isOk());

        // 3. POST /incidents -> 403 Forbidden
        mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "Unauthorized incident",
                                "description": "Should fail",
                                "priority": "CRITICAL",
                                "location": "Noida"
                            }
                            """))
                .andExpect(status().isForbidden());

        // 4. PATCH /incidents/1/assign -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/incidents/1/assign")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "engineerId": 1
                            }
                            """))
                .andExpect(status().isForbidden());

        // 5. PATCH /incidents/1/status -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/incidents/1/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": "IN_PROGRESS"
                            }
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify Ownership RBAC: Engineer sees only their assigned incidents and can only update their own")
    void shouldEnforceOwnershipBasedAccessForEngineers() throws Exception {
        // Setup two engineers
        Engineer eng1 = new Engineer();
        eng1.setName("Engineer One");
        eng1.setEmail("eng1@test.com");
        eng1.setSpecialization("Fibre");
        Engineer savedEng1 = engineerRepository.save(eng1);

        Engineer eng2 = new Engineer();
        eng2.setName("Engineer Two");
        eng2.setEmail("eng2@test.com");
        eng2.setSpecialization("Router");
        Engineer savedEng2 = engineerRepository.save(eng2);

        // Setup users linked to engineers
        userService.createUser(new AdminCreateUserRequest("user_eng1", "user_eng1@test.com", "pass1234", Role.ROLE_ENGINEER, savedEng1.getId()));
        userService.createUser(new AdminCreateUserRequest("user_eng2", "user_eng2@test.com", "pass1234", Role.ROLE_ENGINEER, savedEng2.getId()));

        AuthResponse eng1Auth = authService.login(new LoginRequest("user_eng1", "pass1234"));
        String tokenEng1 = "Bearer " + eng1Auth.getToken();

        // Create incident assigned to Eng 1
        Incident incidentEng1 = new Incident();
        incidentEng1.setTitle("Eng1 ticket");
        incidentEng1.setDescription("Description");
        incidentEng1.setPriority(IncidentPriority.HIGH);
        incidentEng1.setStatus(IncidentStatus.ASSIGNED);
        incidentEng1.setLocation("Delhi");
        incidentEng1.setAssignedEngineer(savedEng1);
        Incident savedInc1 = incidentRepository.save(incidentEng1);

        // Create incident assigned to Eng 2
        Incident incidentEng2 = new Incident();
        incidentEng2.setTitle("Eng2 ticket");
        incidentEng2.setDescription("Description");
        incidentEng2.setPriority(IncidentPriority.CRITICAL);
        incidentEng2.setStatus(IncidentStatus.ASSIGNED);
        incidentEng2.setLocation("Mumbai");
        incidentEng2.setAssignedEngineer(savedEng2);
        Incident savedInc2 = incidentRepository.save(incidentEng2);

        // 1. GET /api/v1/incidents for Eng1 returns ONLY incident 1
        mockMvc.perform(get("/api/v1/incidents").header("Authorization", tokenEng1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(savedInc1.getId()))
                .andExpect(jsonPath("$.content[0].title").value("Eng1 ticket"));

        // 2. GET /api/v1/incidents/mine returns ONLY incident 1
        mockMvc.perform(get("/api/v1/incidents/mine").header("Authorization", tokenEng1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(savedInc1.getId()));

        // 3. Eng1 updates their own incident -> 200 OK
        mockMvc.perform(patch("/api/v1/incidents/" + savedInc1.getId() + "/status")
                        .header("Authorization", tokenEng1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": "IN_PROGRESS"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 4. Eng1 tries to update Eng2's incident -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/incidents/" + savedInc2.getId() + "/status")
                        .header("Authorization", tokenEng1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": "IN_PROGRESS"
                            }
                            """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Engineers can only update status on incidents assigned to them"));
    }

    @Test
    @DisplayName("Verify unassign endpoint security: ADMIN and MANAGER allowed, ENGINEER and VIEWER forbidden")
    void shouldEnforceRbacOnUnassignEndpoint() throws Exception {
        User admin = userRepository.save(new User("admin_unassign", "admin_u@test.com", "pass", Role.ROLE_ADMIN));
        User manager = userRepository.save(new User("manager_unassign", "manager_u@test.com", "pass", Role.ROLE_MANAGER));
        User engineer = userRepository.save(new User("eng_unassign", "eng_u@test.com", "pass", Role.ROLE_ENGINEER));
        User viewer = userRepository.save(new User("viewer_unassign", "viewer_u@test.com", "pass", Role.ROLE_VIEWER));

        String adminToken = "Bearer " + jwtUtils.generateToken(admin);
        String managerToken = "Bearer " + jwtUtils.generateToken(manager);
        String engToken = "Bearer " + jwtUtils.generateToken(engineer);
        String viewerToken = "Bearer " + jwtUtils.generateToken(viewer);

        Engineer tech = new Engineer();
        tech.setName("Unassign Tech");
        tech.setEmail("unatech@test.com");
        tech.setSpecialization("Wireless");
        tech = engineerRepository.save(tech);

        Incident incident = new Incident();
        incident.setTitle("Target incident");
        incident.setDescription("Desc");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignedEngineer(tech);
        Incident saved = incidentRepository.save(incident);

        // 1. Unauthenticated -> 401
        mockMvc.perform(patch("/api/v1/incidents/" + saved.getId() + "/unassign"))
                .andExpect(status().isUnauthorized());

        // 2. ENGINEER -> 403
        mockMvc.perform(patch("/api/v1/incidents/" + saved.getId() + "/unassign")
                        .header("Authorization", engToken))
                .andExpect(status().isForbidden());

        // 3. VIEWER -> 403
        mockMvc.perform(patch("/api/v1/incidents/" + saved.getId() + "/unassign")
                        .header("Authorization", viewerToken))
                .andExpect(status().isForbidden());

        // 4. MANAGER -> 200 OK
        mockMvc.perform(patch("/api/v1/incidents/" + saved.getId() + "/unassign")
                        .header("Authorization", managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedEngineerId").doesNotExist());
    }

    @Test
    @DisplayName("Verify RBAC on PUT /engineers/{id} and PATCH /engineers/{id}/active: ADMIN and MANAGER allowed, ENGINEER and VIEWER forbidden")
    void shouldEnforceRbacOnEngineerUpdateAndActiveEndpoints() throws Exception {
        User admin = userRepository.save(new User("admin_eng_rbac", "admin_eng@test.com", "pass", Role.ROLE_ADMIN));
        User manager = userRepository.save(new User("mgr_eng_rbac", "mgr_eng@test.com", "pass", Role.ROLE_MANAGER));
        User engineer = userRepository.save(new User("tech_eng_rbac", "tech_eng@test.com", "pass", Role.ROLE_ENGINEER));
        User viewer = userRepository.save(new User("view_eng_rbac", "view_eng@test.com", "pass", Role.ROLE_VIEWER));

        String adminToken = "Bearer " + jwtUtils.generateToken(admin);
        String managerToken = "Bearer " + jwtUtils.generateToken(manager);
        String engToken = "Bearer " + jwtUtils.generateToken(engineer);
        String viewerToken = "Bearer " + jwtUtils.generateToken(viewer);

        Engineer tech = new Engineer();
        tech.setName("RBAC Target Tech");
        tech.setEmail("rbactarget@test.com");
        tech.setSpecialization("Core Fibre");
        tech = engineerRepository.save(tech);
        Long id = tech.getId();

        String updateJson = """
            {
                "name": "Updated Tech",
                "phone": "+91-98765-00000",
                "specialization": "DWDM",
                "availabilityStatus": "BUSY"
            }
            """;

        // PUT unauthenticated -> 401
        mockMvc.perform(put("/api/v1/engineers/" + id).contentType(MediaType.APPLICATION_JSON).content(updateJson))
                .andExpect(status().isUnauthorized());

        // PUT ENGINEER -> 403
        mockMvc.perform(put("/api/v1/engineers/" + id).header("Authorization", engToken).contentType(MediaType.APPLICATION_JSON).content(updateJson))
                .andExpect(status().isForbidden());

        // PUT VIEWER -> 403
        mockMvc.perform(put("/api/v1/engineers/" + id).header("Authorization", viewerToken).contentType(MediaType.APPLICATION_JSON).content(updateJson))
                .andExpect(status().isForbidden());

        // PUT MANAGER -> 200 OK
        mockMvc.perform(put("/api/v1/engineers/" + id).header("Authorization", managerToken).contentType(MediaType.APPLICATION_JSON).content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Tech"))
                .andExpect(jsonPath("$.availabilityStatus").value("BUSY"));

        // PATCH /active unauthenticated -> 401
        mockMvc.perform(patch("/api/v1/engineers/" + id + "/active?active=false"))
                .andExpect(status().isUnauthorized());

        // PATCH /active ENGINEER -> 403
        mockMvc.perform(patch("/api/v1/engineers/" + id + "/active?active=false").header("Authorization", engToken))
                .andExpect(status().isForbidden());

        // PATCH /active ADMIN -> 200 OK
        mockMvc.perform(patch("/api/v1/engineers/" + id + "/active?active=false").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("Verify service visit RBAC: ADMIN/MANAGER/assigned ENGINEER allowed, VIEWER and peer ENGINEER forbidden")
    void shouldEnforceRbacOnServiceVisitsEndpoints() throws Exception {
        Engineer eng1 = new Engineer();
        eng1.setName("Eng One Visit");
        eng1.setEmail("eng1.visit@test.com");
        eng1.setSpecialization("Optical");
        eng1 = engineerRepository.save(eng1);

        Engineer eng2 = new Engineer();
        eng2.setName("Eng Two Visit");
        eng2.setEmail("eng2.visit@test.com");
        eng2.setSpecialization("Core");
        eng2 = engineerRepository.save(eng2);

        User userEng1 = new User("user_eng1_visit", "eng1_v@test.com", "pass", Role.ROLE_ENGINEER);
        userEng1.setEngineer(eng1);
        userEng1 = userRepository.save(userEng1);

        User userEng2 = new User("user_eng2_visit", "eng2_v@test.com", "pass", Role.ROLE_ENGINEER);
        userEng2.setEngineer(eng2);
        userEng2 = userRepository.save(userEng2);

        User manager = userRepository.save(new User("manager_visit", "manager_v@test.com", "pass", Role.ROLE_MANAGER));
        User viewer = userRepository.save(new User("viewer_visit", "viewer_v@test.com", "pass", Role.ROLE_VIEWER));

        String tokenEng1 = "Bearer " + jwtUtils.generateToken(userEng1);
        String tokenEng2 = "Bearer " + jwtUtils.generateToken(userEng2);
        String tokenManager = "Bearer " + jwtUtils.generateToken(manager);
        String tokenViewer = "Bearer " + jwtUtils.generateToken(viewer);

        Incident incident = new Incident();
        incident.setTitle("Visit security test ticket");
        incident.setDescription("Desc");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignedEngineer(eng1);
        Incident savedInc = incidentRepository.save(incident);

        String visitPayload = """
            {
                "visitDate": "2026-10-29",
                "workNotes": "On-site optical inspection completed"
            }
            """;

        // 1. Unauthenticated -> 401
        mockMvc.perform(post("/api/v1/incidents/" + savedInc.getId() + "/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(visitPayload))
                .andExpect(status().isUnauthorized());

        // 2. VIEWER -> 403
        mockMvc.perform(post("/api/v1/incidents/" + savedInc.getId() + "/visits")
                        .header("Authorization", tokenViewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(visitPayload))
                .andExpect(status().isForbidden());

        // 3. Peer ENGINEER (eng2) on eng1's incident -> 403 Forbidden
        mockMvc.perform(post("/api/v1/incidents/" + savedInc.getId() + "/visits")
                        .header("Authorization", tokenEng2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(visitPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Engineers can only log service visits on incidents assigned to them"));

        // 4. Assigned ENGINEER (eng1) -> 201 Created
        mockMvc.perform(post("/api/v1/incidents/" + savedInc.getId() + "/visits")
                        .header("Authorization", tokenEng1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(visitPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId").value(savedInc.getId()))
                .andExpect(jsonPath("$.engineerId").value(eng1.getId()))
                .andExpect(jsonPath("$.workNotes").value("On-site optical inspection completed"));

        // 5. MANAGER -> 201 Created
        mockMvc.perform(post("/api/v1/incidents/" + savedInc.getId() + "/visits")
                        .header("Authorization", tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "visitDate": "2026-10-29",
                                "workNotes": "Manager supervisory inspection"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workNotes").value("Manager supervisory inspection"));

        // 6. GET visits -> VIEWER allowed to view (200 OK)
        mockMvc.perform(get("/api/v1/incidents/" + savedInc.getId() + "/visits")
                        .header("Authorization", tokenViewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Verify comments & timeline RBAC: ADMIN/MANAGER/assigned ENGINEER allowed to comment, VIEWER/peer ENGINEER forbidden")
    void shouldEnforceRbacOnCommentsAndTimelineEndpoints() throws Exception {
        Engineer eng1 = new Engineer();
        eng1.setName("Comment Eng One");
        eng1.setEmail("ceng1@test.com");
        eng1.setSpecialization("Optical");
        eng1 = engineerRepository.save(eng1);

        Engineer eng2 = new Engineer();
        eng2.setName("Comment Eng Two");
        eng2.setEmail("ceng2@test.com");
        eng2.setSpecialization("Core");
        eng2 = engineerRepository.save(eng2);

        User userEng1 = new User("user_eng1_comment", "ceng1_u@test.com", "pass", Role.ROLE_ENGINEER, eng1);
        userRepository.save(userEng1);

        User userEng2 = new User("user_eng2_comment", "ceng2_u@test.com", "pass", Role.ROLE_ENGINEER, eng2);
        userRepository.save(userEng2);

        User manager = userRepository.save(new User("manager_comment", "mgr_c@test.com", "pass", Role.ROLE_MANAGER));
        User viewer = userRepository.save(new User("viewer_comment", "view_c@test.com", "pass", Role.ROLE_VIEWER));

        String tokenEng1 = "Bearer " + jwtUtils.generateToken(userEng1);
        String tokenEng2 = "Bearer " + jwtUtils.generateToken(userEng2);
        String tokenManager = "Bearer " + jwtUtils.generateToken(manager);
        String tokenViewer = "Bearer " + jwtUtils.generateToken(viewer);

        Incident incident = new Incident();
        incident.setTitle("Comment security test ticket");
        incident.setDescription("Desc");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignedEngineer(eng1);
        Incident savedInc = incidentRepository.save(incident);

        String commentPayload = """
            {
                "text": "Tested primary fiber link signal strength"
            }
            """;

        // 1. POST comment unauthenticated -> 401
        mockMvc.perform(post("/api/v1/incidents/" + savedInc.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentPayload))
                .andExpect(status().isUnauthorized());

        // 2. POST comment VIEWER -> 403 Forbidden
        mockMvc.perform(post("/api/v1/incidents/" + savedInc.getId() + "/comments")
                        .header("Authorization", tokenViewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentPayload))
                .andExpect(status().isForbidden());

        // 3. POST comment peer ENGINEER (eng2) on eng1's incident -> 403 Forbidden
        mockMvc.perform(post("/api/v1/incidents/" + savedInc.getId() + "/comments")
                        .header("Authorization", tokenEng2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Engineers can only comment on incidents assigned to them"));

        // 4. POST comment assigned ENGINEER (eng1) -> 201 Created
        mockMvc.perform(post("/api/v1/incidents/" + savedInc.getId() + "/comments")
                        .header("Authorization", tokenEng1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("user_eng1_comment"))
                .andExpect(jsonPath("$.text").value("Tested primary fiber link signal strength"));

        // 5. POST comment MANAGER -> 201 Created
        mockMvc.perform(post("/api/v1/incidents/" + savedInc.getId() + "/comments")
                        .header("Authorization", tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "text": "Manager escalation note"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("manager_comment"));

        // 6. GET comments -> VIEWER allowed (200 OK)
        mockMvc.perform(get("/api/v1/incidents/" + savedInc.getId() + "/comments")
                        .header("Authorization", tokenViewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        // 7. GET timeline -> VIEWER allowed (200 OK)
        mockMvc.perform(get("/api/v1/incidents/" + savedInc.getId() + "/timeline")
                        .header("Authorization", tokenViewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.itemType == 'COMMENT')]").exists());
    }
}
