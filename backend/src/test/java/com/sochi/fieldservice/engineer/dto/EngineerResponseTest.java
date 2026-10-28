package com.sochi.fieldservice.engineer.dto;

import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import com.sochi.fieldservice.engineer.entity.Engineer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EngineerResponseTest {

    @Test
    @DisplayName("Verify EngineerResponse maps all fields and workload accurately")
    void shouldMapAllFieldsFromEngineerAndWorkload() {
        Engineer engineer = new Engineer();
        engineer.setId(42L);
        engineer.setName("Suresh Raina");
        engineer.setEmail("suresh@telecom.net");
        engineer.setSpecialization("Cell Site BTS");
        engineer.setPhone("+91-99887-76655");
        engineer.setActive(true);
        engineer.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        LocalDateTime now = LocalDateTime.now();
        engineer.setCreatedAt(now);

        EngineerResponse response = new EngineerResponse(engineer, 2L);

        assertEquals(42L, response.getId());
        assertEquals("Suresh Raina", response.getName());
        assertEquals("suresh@telecom.net", response.getEmail());
        assertEquals("+91-99887-76655", response.getPhone());
        assertEquals("Cell Site BTS", response.getSpecialization());
        assertTrue(response.isActive());
        assertEquals(AvailabilityStatus.AVAILABLE, response.getAvailabilityStatus());
        assertEquals(2L, response.getCurrentWorkload());
        assertEquals(now, response.getCreatedAt());

        // Test single-argument constructor defaults workload to 0
        EngineerResponse responseDefault = new EngineerResponse(engineer);
        assertEquals(0L, responseDefault.getCurrentWorkload());
    }
}
