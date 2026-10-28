package com.sochi.fieldservice.user.controller;

import com.sochi.fieldservice.security.SecurityConfig;
import com.sochi.fieldservice.user.dto.AdminCreateUserRequest;
import com.sochi.fieldservice.user.dto.UserResponse;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminUserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldCreateUserSuccessfully() throws Exception {
        UserResponse response = new UserResponse(
                5L,
                "dispatcher1",
                "dispatcher1@example.com",
                Role.ROLE_MANAGER,
                true,
                LocalDateTime.now()
        );

        when(userService.createUser(any(AdminCreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "dispatcher1",
                                "email": "dispatcher1@example.com",
                                "password": "password123",
                                "role": "ROLE_MANAGER"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("dispatcher1"))
                .andExpect(jsonPath("$.email").value("dispatcher1@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_MANAGER"))
                .andExpect(jsonPath("$.enabled").value(true));

        verify(userService).createUser(any(AdminCreateUserRequest.class));
    }

    @Test
    void shouldRejectWhenUsernameIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "",
                                "email": "dispatcher1@example.com",
                                "password": "password123",
                                "role": "ROLE_MANAGER"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectWhenRoleIsNull() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "dispatcher1",
                                "email": "dispatcher1@example.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }
}
