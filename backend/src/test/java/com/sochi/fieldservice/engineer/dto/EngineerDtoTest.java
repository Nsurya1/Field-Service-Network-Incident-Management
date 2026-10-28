package com.sochi.fieldservice.engineer.dto;

import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineerDtoTest {

    @Test
    @DisplayName("Verify UpdateEngineerRequest getters, setters and constructor")
    void shouldSupportUpdateEngineerRequest() {
        UpdateEngineerRequest request = new UpdateEngineerRequest(
                "Anita Bose", "+91-91234-56789", "Optical Transport", AvailabilityStatus.BUSY
        );

        assertEquals("Anita Bose", request.getName());
        assertEquals("+91-91234-56789", request.getPhone());
        assertEquals("Optical Transport", request.getSpecialization());
        assertEquals(AvailabilityStatus.BUSY, request.getAvailabilityStatus());

        request.setName("Anita Desai");
        request.setPhone("+91-99999-88888");
        request.setSpecialization("Core IP");
        request.setAvailabilityStatus(AvailabilityStatus.OFF_DUTY);

        assertEquals("Anita Desai", request.getName());
        assertEquals("+91-99999-88888", request.getPhone());
        assertEquals("Core IP", request.getSpecialization());
        assertEquals(AvailabilityStatus.OFF_DUTY, request.getAvailabilityStatus());
    }

    @Test
    @DisplayName("Verify UpdateEngineerActiveRequest getters and setters")
    void shouldSupportUpdateEngineerActiveRequest() {
        UpdateEngineerActiveRequest request = new UpdateEngineerActiveRequest(true);
        assertTrue(request.getActive());

        request.setActive(false);
        assertFalse(request.getActive());

        UpdateEngineerActiveRequest empty = new UpdateEngineerActiveRequest();
        assertNull(empty.getActive());
    }

    @Test
    @DisplayName("Verify CreateEngineerRequest constructors with phone")
    void shouldSupportCreateEngineerRequestWithPhone() {
        CreateEngineerRequest req1 = new CreateEngineerRequest("Name", "email@test.com", "Fibre");
        assertNull(req1.getPhone());

        CreateEngineerRequest req2 = new CreateEngineerRequest("Name", "email@test.com", "Fibre", "+91-12345");
        assertEquals("+91-12345", req2.getPhone());

        req2.setPhone("+91-54321");
        assertEquals("+91-54321", req2.getPhone());
    }
}
