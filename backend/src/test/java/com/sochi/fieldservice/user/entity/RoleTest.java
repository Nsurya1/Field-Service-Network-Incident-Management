package com.sochi.fieldservice.user.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RoleTest {

    @Test
    void shouldHaveExpectedRoles() {
        assertEquals(3, Role.values().length);
        assertNotNull(Role.valueOf("ROLE_ADMIN"));
        assertNotNull(Role.valueOf("ROLE_MANAGER"));
        assertNotNull(Role.valueOf("ROLE_ENGINEER"));
    }
}
