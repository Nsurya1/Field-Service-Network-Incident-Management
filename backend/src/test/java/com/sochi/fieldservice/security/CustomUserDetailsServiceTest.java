package com.sochi.fieldservice.security;

import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldLoadUserByUsername() {
        User user = new User("admin", "admin@example.com", "pass", Role.ROLE_ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("admin");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        verify(userRepository).findByUsername("admin");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void shouldLoadUserByEmailWhenUsernameNotFound() {
        User user = new User("admin", "admin@example.com", "pass", Role.ROLE_ADMIN);
        when(userRepository.findByUsername("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("admin@example.com");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        verify(userRepository).findByUsername("admin@example.com");
        verify(userRepository).findByEmail("admin@example.com");
    }

    @Test
    void shouldThrowUsernameNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown")
        );

        verify(userRepository).findByUsername("unknown");
        verify(userRepository).findByEmail("unknown");
    }
}
