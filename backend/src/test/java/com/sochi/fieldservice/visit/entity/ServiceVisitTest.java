package com.sochi.fieldservice.visit.entity;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.incident.entity.Incident;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ServiceVisitTest {

    @Test
    @DisplayName("Verify onCreate initializes createdAt and defaults visitDate to today if null")
    void onCreateShouldInitializeTimestamps() {
        ServiceVisit visit = new ServiceVisit();
        assertNull(visit.getCreatedAt());
        assertNull(visit.getVisitDate());

        visit.onCreate();

        assertNotNull(visit.getCreatedAt());
        assertEquals(LocalDate.now(), visit.getVisitDate());
    }

    @Test
    @DisplayName("Verify getters, setters, and parameterized constructor")
    void shouldSupportGettersAndSetters() {
        Incident incident = new Incident();
        incident.setId(10L);
        Engineer engineer = new Engineer();
        engineer.setId(5L);

        LocalDate date = LocalDate.of(2026, 10, 29);
        ServiceVisit visit = new ServiceVisit(incident, engineer, date, "Replaced blown SFP+ module");
        visit.setId(100L);

        LocalDateTime checkIn = LocalDateTime.of(2026, 10, 29, 10, 0);
        LocalDateTime checkOut = LocalDateTime.of(2026, 10, 29, 11, 30);
        visit.setCheckInAt(checkIn);
        visit.setCheckOutAt(checkOut);
        visit.setResolutionNotes("Link restored with 0dBm optical power");
        LocalDateTime created = LocalDateTime.now();
        visit.setCreatedAt(created);

        assertEquals(100L, visit.getId());
        assertEquals(incident, visit.getIncident());
        assertEquals(engineer, visit.getEngineer());
        assertEquals(date, visit.getVisitDate());
        assertEquals(checkIn, visit.getCheckInAt());
        assertEquals(checkOut, visit.getCheckOutAt());
        assertEquals("Replaced blown SFP+ module", visit.getWorkNotes());
        assertEquals("Link restored with 0dBm optical power", visit.getResolutionNotes());
        assertEquals(created, visit.getCreatedAt());
    }
}
