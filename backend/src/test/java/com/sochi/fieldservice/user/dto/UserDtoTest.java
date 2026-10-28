package com.sochi.fieldservice.user.dto;

import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserDtoTest {

    @Test
    void shouldSupportAdminCreateUserRequest() {
        AdminCreateUserRequest req = new AdminCreateUserRequest("admin_user", "admin@example.com", "pass123", Role.ROLE_ADMIN);
        assertEquals("admin_user", req.getUsername());
        assertEquals("admin@example.com", req.getEmail());
        assertEquals("pass123", req.getPassword());
        assertEquals(Role.ROLE_ADMIN, req.getRole());

        AdminCreateUserRequest req2 = new AdminCreateUserRequest();
        req2.setUsername("tech");
        req2.setEmail("tech@example.com");
        req2.setPassword("secret");
        req2.setRole(Role.ROLE_ENGINEER);
        assertEquals("tech", req2.getUsername());
        assertEquals("tech@example.com", req2.getEmail());
        assertEquals("secret", req2.getPassword());
        assertEquals(Role.ROLE_ENGINEER, req2.getRole());
        AdminCreateUserRequest req3 = new AdminCreateUserRequest("tech_eng", "tech_eng@example.com", "pass", Role.ROLE_ENGINEER, 77L);
        assertEquals(77L, req3.getEngineerId());
        req2.setEngineerId(88L);
        assertEquals(88L, req2.getEngineerId());
    }

    @Test
    void shouldSupportUserResponse() {
        LocalDateTime now = LocalDateTime.now();
        UserResponse response = new UserResponse(1L, "admin_user", "admin@example.com", Role.ROLE_ADMIN, true, now, 55L);
        assertEquals(1L, response.getId());
        assertEquals("admin_user", response.getUsername());
        assertEquals("admin@example.com", response.getEmail());
        assertEquals(Role.ROLE_ADMIN, response.getRole());
        assertTrue(response.isEnabled());
        assertEquals(now, response.getCreatedAt());
        assertEquals(55L, response.getEngineerId());

        com.sochi.fieldservice.engineer.entity.Engineer eng = new com.sochi.fieldservice.engineer.entity.Engineer();
        org.springframework.test.util.ReflectionTestUtils.setField(eng, "id", 99L);
        User user = new User("manager1", "manager@example.com", "hash", Role.ROLE_MANAGER, eng);
        user.setId(2L);
        user.setCreatedAt(now);

        UserResponse mapped = UserResponse.fromEntity(user);
        assertEquals(2L, mapped.getId());
        assertEquals(99L, mapped.getEngineerId());

        UserResponse response2 = new UserResponse();
        response2.setEngineerId(101L);
        assertEquals(101L, response2.getEngineerId());
        response2.setId(3L);
        response2.setUsername("eng");
        response2.setEmail("eng@example.com");
        response2.setRole(Role.ROLE_ENGINEER);
        response2.setEnabled(false);
        response2.setCreatedAt(now);
        assertEquals(3L, response2.getId());
        assertEquals("eng", response2.getUsername());
        assertEquals("eng@example.com", response2.getEmail());
        assertEquals(Role.ROLE_ENGINEER, response2.getRole());
        assertFalse(response2.isEnabled());
        assertEquals(now, response2.getCreatedAt());
    }
}
