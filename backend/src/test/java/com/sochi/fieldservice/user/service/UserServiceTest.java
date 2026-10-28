package com.sochi.fieldservice.user.service;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EngineerRepository engineerRepository;

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
    void createUserWithValidEngineerIdShouldLinkEngineer() {
        AdminCreateUserRequest request = new AdminCreateUserRequest("tech_rajesh", "tech@example.com", "pass1234", Role.ROLE_ENGINEER, 5L);

        Engineer engineer = new Engineer();
        engineer.setName("Rajesh Sharma");
        org.springframework.test.util.ReflectionTestUtils.setField(engineer, "id", 5L);

        when(userRepository.existsByUsername("tech_rajesh")).thenReturn(false);
        when(userRepository.existsByEmail("tech@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("hashed-pwd");
        when(engineerRepository.findById(5L)).thenReturn(Optional.of(engineer));
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(12L);
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals(12L, response.getId());
        assertEquals(5L, response.getEngineerId());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(engineer, captor.getValue().getEngineer());
    }

    @Test
    void createUserWithNonExistentEngineerIdShouldThrow404() {
        AdminCreateUserRequest request = new AdminCreateUserRequest("tech_invalid", "tech2@example.com", "pass1234", Role.ROLE_ENGINEER, 999L);

        when(userRepository.existsByUsername("tech_invalid")).thenReturn(false);
        when(userRepository.existsByEmail("tech2@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("hashed-pwd");
        when(engineerRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.createUser(request));
        assertEquals(404, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Engineer not found"));
        verify(userRepository, never()).save(any(User.class));
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
