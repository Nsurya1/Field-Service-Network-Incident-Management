package com.sochi.fieldservice.security;

import com.sochi.fieldservice.security.dto.AuthResponse;
import com.sochi.fieldservice.security.dto.LoginRequest;
import com.sochi.fieldservice.security.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldRegisterNewUserAsEngineer() throws Exception {
        AuthResponse response = new AuthResponse("token-abc", "diana", "diana@example.com", "ROLE_ENGINEER");

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "diana",
                                "email": "diana@example.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-abc"))
                .andExpect(jsonPath("$.username").value("diana"))
                .andExpect(jsonPath("$.email").value("diana@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_ENGINEER"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldIgnoreRoleFieldWhenPassedToPublicRegister() throws Exception {
        AuthResponse response = new AuthResponse("token-abc", "hacker", "hacker@example.com", "ROLE_ENGINEER");

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "hacker",
                                "email": "hacker@example.com",
                                "password": "password123",
                                "role": "ROLE_ADMIN"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_ENGINEER"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldRejectRegisterWhenUsernameIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "",
                                "email": "diana@example.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        AuthResponse response = new AuthResponse("token-xyz", "diana", "diana@example.com", "ROLE_ADMIN");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "diana",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-xyz"))
                .andExpect(jsonPath("$.username").value("diana"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldRejectLoginWhenPasswordIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "diana",
                                "password": ""
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleForgotPasswordSuccessfully() throws Exception {
        when(authService.forgotPassword(any())).thenReturn(
                new com.sochi.fieldservice.security.dto.ForgotPasswordResponse("Code generated", "TOKEN123")
        );

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "usernameOrEmail": "diana"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Code generated"))
                .andExpect(jsonPath("$.resetToken").value("TOKEN123"));
    }

    @Test
    void shouldHandleResetPasswordSuccessfully() throws Exception {
        when(authService.resetPassword(any())).thenReturn(
                new com.sochi.fieldservice.security.dto.MessageResponse("Password reset successfully")
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "resetToken": "TOKEN123",
                                "newPassword": "newPassword123"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }
}
