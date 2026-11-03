package com.sochi.fieldservice.security.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthDtoTest {

    @Test
    void shouldSupportRegisterRequest() {
        RegisterRequest req = new RegisterRequest("john", "john@example.com", "pass123");
        assertEquals("john", req.getUsername());
        assertEquals("john@example.com", req.getEmail());
        assertEquals("pass123", req.getPassword());

        RegisterRequest req2 = new RegisterRequest();
        req2.setUsername("bob");
        req2.setEmail("bob@example.com");
        req2.setPassword("secret");
        assertEquals("bob", req2.getUsername());
        assertEquals("bob@example.com", req2.getEmail());
        assertEquals("secret", req2.getPassword());
    }

    @Test
    void shouldSupportLoginRequest() {
        LoginRequest req = new LoginRequest("john", "pass123");
        assertEquals("john", req.getUsername());
        assertEquals("pass123", req.getPassword());

        LoginRequest req2 = new LoginRequest();
        req2.setUsername("bob");
        req2.setPassword("secret");
        assertEquals("bob", req2.getUsername());
        assertEquals("secret", req2.getPassword());
    }

    @Test
    void shouldSupportAuthResponse() {
        AuthResponse res = new AuthResponse("jwt.token.here", "john", "john@example.com", "ROLE_ADMIN");
        assertEquals("jwt.token.here", res.getToken());
        assertEquals("Bearer", res.getType());
        assertEquals("john", res.getUsername());
        assertEquals("john@example.com", res.getEmail());
        assertEquals("ROLE_ADMIN", res.getRole());

        AuthResponse res2 = new AuthResponse();
        res2.setToken("tok");
        res2.setType("Bearer");
        res2.setUsername("bob");
        res2.setEmail("bob@example.com");
        res2.setRole("ROLE_MANAGER");
        assertEquals("tok", res2.getToken());
        assertEquals("Bearer", res2.getType());
        assertEquals("bob", res2.getUsername());
        assertEquals("bob@example.com", res2.getEmail());
        assertEquals("ROLE_MANAGER", res2.getRole());
    }

    @Test
    void shouldSupportForgotPasswordDtos() {
        ForgotPasswordRequest req = new ForgotPasswordRequest("john");
        assertEquals("john", req.getUsernameOrEmail());

        ForgotPasswordRequest req2 = new ForgotPasswordRequest();
        req2.setUsernameOrEmail("john@example.com");
        assertEquals("john@example.com", req2.getUsernameOrEmail());

        ForgotPasswordResponse res = new ForgotPasswordResponse("Reset sent", "TOKEN123");
        assertEquals("Reset sent", res.getMessage());
        assertEquals("TOKEN123", res.getResetToken());

        ForgotPasswordResponse res2 = new ForgotPasswordResponse();
        res2.setMessage("msg");
        res2.setResetToken("tok");
        assertEquals("msg", res2.getMessage());
        assertEquals("tok", res2.getResetToken());
    }

    @Test
    void shouldSupportResetPasswordDtos() {
        ResetPasswordRequest req = new ResetPasswordRequest("TOKEN", "pass123");
        assertEquals("TOKEN", req.getResetToken());
        assertEquals("pass123", req.getNewPassword());

        ResetPasswordRequest req2 = new ResetPasswordRequest();
        req2.setResetToken("TOKEN2");
        req2.setNewPassword("pass456");
        assertEquals("TOKEN2", req2.getResetToken());
        assertEquals("pass456", req2.getNewPassword());

        MessageResponse msg = new MessageResponse("Success");
        assertEquals("Success", msg.getMessage());

        MessageResponse msg2 = new MessageResponse();
        msg2.setMessage("Updated");
        assertEquals("Updated", msg2.getMessage());
    }
}
