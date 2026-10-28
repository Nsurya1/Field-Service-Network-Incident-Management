package com.sochi.fieldservice.security;

import com.sochi.fieldservice.security.dto.AuthResponse;
import com.sochi.fieldservice.security.dto.LoginRequest;
import com.sochi.fieldservice.security.dto.RegisterRequest;
import com.sochi.fieldservice.security.jwt.JwtUtils;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldSaveUserAsEngineerAndReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest("diana", "diana@example.com", "secret123");

        when(userRepository.existsByUsername("diana")).thenReturn(false);
        when(userRepository.existsByEmail("diana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtUtils.generateToken(any(User.class))).thenReturn("jwt-token-123");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token-123", response.getToken());
        assertEquals("diana", response.getUsername());
        assertEquals("diana@example.com", response.getEmail());
        assertEquals("ROLE_ENGINEER", response.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.ROLE_ENGINEER, userCaptor.getValue().getRole());
    }

    @Test
    void registerShouldThrow409WhenUsernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest("diana", "diana@example.com", "secret123");

        when(userRepository.existsByUsername("diana")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(request)
        );

        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Username already taken"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerShouldThrow409WhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("diana", "diana@example.com", "secret123");

        when(userRepository.existsByUsername("diana")).thenReturn(false);
        when(userRepository.existsByEmail("diana@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(request)
        );

        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Email already in use"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginShouldAuthenticateAndReturnAuthResponse() {
        LoginRequest request = new LoginRequest("diana", "secret123");
        User user = new User("diana", "diana@example.com", "hashed", Role.ROLE_ADMIN);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateToken(user)).thenReturn("jwt-token-login");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token-login", response.getToken());
        assertEquals("diana", response.getUsername());
        assertEquals("diana@example.com", response.getEmail());
        assertEquals("ROLE_ADMIN", response.getRole());
    }

    @Test
    void loginShouldThrow401WhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest("diana", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertEquals(401, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Invalid username or password"));
    }
}
