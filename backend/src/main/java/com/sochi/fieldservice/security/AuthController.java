package com.sochi.fieldservice.security;

import com.sochi.fieldservice.security.dto.AuthResponse;
import com.sochi.fieldservice.security.dto.ForgotPasswordRequest;
import com.sochi.fieldservice.security.dto.ForgotPasswordResponse;
import com.sochi.fieldservice.security.dto.LoginRequest;
import com.sochi.fieldservice.security.dto.MessageResponse;
import com.sochi.fieldservice.security.dto.RegisterRequest;
import com.sochi.fieldservice.security.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
