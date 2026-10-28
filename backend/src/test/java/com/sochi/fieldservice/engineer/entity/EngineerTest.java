package com.sochi.fieldservice.engineer.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EngineerTest {

    @Test
    @DisplayName("Verify onCreate initializes createdAt and defaults availabilityStatus to AVAILABLE")
    void onCreateShouldInitializeCreatedAtAndDefaultAvailability() {
        Engineer engineer = new Engineer();
        assertNull(engineer.getCreatedAt());
        assertTrue(engineer.isActive(), "Engineer active flag must default to true");
        assertEquals(AvailabilityStatus.AVAILABLE, engineer.getAvailabilityStatus());

        engineer.setAvailabilityStatus(null);
        engineer.onCreate();

        assertNotNull(engineer.getCreatedAt());
        assertEquals(AvailabilityStatus.AVAILABLE, engineer.getAvailabilityStatus());
    }

    @Test
    @DisplayName("Verify getters and setters for all engineer fields")
    void shouldSupportGettersAndSetters() {
        Engineer engineer = new Engineer();
        engineer.setId(10L);
        engineer.setName("Vikram Malhotra");
        engineer.setEmail("vikram@telecom.net");
        engineer.setSpecialization("DWDM");
        engineer.setPhone("+91-98765-43210");
        engineer.setActive(false);
        engineer.setAvailabilityStatus(AvailabilityStatus.BUSY);
        LocalDateTime now = LocalDateTime.now();
        engineer.setCreatedAt(now);

        assertEquals(10L, engineer.getId());
        assertEquals("Vikram Malhotra", engineer.getName());
        assertEquals("vikram@telecom.net", engineer.getEmail());
        assertEquals("DWDM", engineer.getSpecialization());
        assertEquals("+91-98765-43210", engineer.getPhone());
        assertFalse(engineer.isActive());
        assertEquals(AvailabilityStatus.BUSY, engineer.getAvailabilityStatus());
        assertEquals(now, engineer.getCreatedAt());
    }
}
