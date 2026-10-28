package com.sochi.fieldservice.user.entity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserWithAllArgsConstructor() {
        User user = new User("john_doe", "john@example.com", "secret123", Role.ROLE_ADMIN);

        assertEquals("john_doe", user.getUsername());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("secret123", user.getPassword());
        assertEquals(Role.ROLE_ADMIN, user.getRole());
        assertTrue(user.isEnabled());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());
    }

    @Test
    void shouldHandleDefaultAuthorityWhenRoleIsNull() {
        User user = new User();
        user.setRole(null);

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.iterator().next().getAuthority());
    }

    @Test
    void shouldSupportSettersAndGetters() {
        User user = new User();
        user.setId(10L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("hashedPass");
        user.setRole(Role.ROLE_MANAGER);
        user.setEnabled(false);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        assertEquals(10L, user.getId());
        assertEquals("alice", user.getUsername());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("hashedPass", user.getPassword());
        assertEquals(Role.ROLE_MANAGER, user.getRole());
        assertFalse(user.isEnabled());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }

    @Test
    void shouldTriggerPrePersistAndPreUpdateLifecycleHooks() {
        User user = new User();
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());

        user.onCreate();
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());

        LocalDateTime created = user.getCreatedAt();
        user.onCreate(); // existing timestamps should not be overwritten
        assertEquals(created, user.getCreatedAt());

        user.onUpdate();
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void shouldSupportEngineerAssociationAndConstructors() {
        com.sochi.fieldservice.engineer.entity.Engineer engineer = new com.sochi.fieldservice.engineer.entity.Engineer();
        engineer.setName("Rajesh");

        User user = new User("tech", "tech@example.com", "secret", Role.ROLE_ENGINEER, engineer);
        assertEquals(engineer, user.getEngineer());

        User user2 = new User();
        user2.setEngineer(engineer);
        assertEquals(engineer, user2.getEngineer());
    }
}
