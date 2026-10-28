package com.sochi.fieldservice.user.service;

import com.sochi.fieldservice.user.dto.AdminCreateUserRequest;
import com.sochi.fieldservice.user.dto.UserResponse;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserShouldSaveAndReturnUserResponseWithGivenRole() {
        AdminCreateUserRequest request = new AdminCreateUserRequest("admin_boss", "boss@example.com", "pass1234", Role.ROLE_ADMIN);

        when(userRepository.existsByUsername("admin_boss")).thenReturn(false);
        when(userRepository.existsByEmail("boss@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("admin_boss", response.getUsername());
        assertEquals("boss@example.com", response.getEmail());
        assertEquals(Role.ROLE_ADMIN, response.getRole());
        assertTrue(response.isEnabled());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("admin_boss", captor.getValue().getUsername());
        assertEquals(Role.ROLE_ADMIN, captor.getValue().getRole());
    }

    @Test
    void createUserShouldDefaultToEngineerIfRoleIsNull() {
        AdminCreateUserRequest request = new AdminCreateUserRequest("user1", "user1@example.com", "pass1234", null);

        when(userRepository.existsByUsername("user1")).thenReturn(false);
        when(userRepository.existsByEmail("user1@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(11L);
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals(Role.ROLE_ENGINEER, response.getRole());
    }

    @Test
    void createUserShouldThrow409WhenUsernameAlreadyExists() {
        AdminCreateUserRequest request = new AdminCreateUserRequest("admin_boss", "boss@example.com", "pass1234", Role.ROLE_ADMIN);

        when(userRepository.existsByUsername("admin_boss")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(request)
        );

        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Username already taken"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserShouldThrow409WhenEmailAlreadyExists() {
        AdminCreateUserRequest request = new AdminCreateUserRequest("admin_boss", "boss@example.com", "pass1234", Role.ROLE_ADMIN);

        when(userRepository.existsByUsername("admin_boss")).thenReturn(false);
        when(userRepository.existsByEmail("boss@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(request)
        );

        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Email already in use"));
        verify(userRepository, never()).save(any(User.class));
    }
}
