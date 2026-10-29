package com.sochi.fieldservice.visit.dto;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.visit.entity.ServiceVisit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ServiceVisitDtoTest {

    @Test
    @DisplayName("Verify CreateServiceVisitRequest constructors and getters/setters")
    void shouldSupportCreateServiceVisitRequest() {
        CreateServiceVisitRequest req = new CreateServiceVisitRequest("Short notes");
        assertEquals("Short notes", req.getWorkNotes());
        assertNull(req.getEngineerId());

        LocalDate visitDate = LocalDate.of(2026, 10, 29);
        LocalDateTime in = LocalDateTime.of(2026, 10, 29, 9, 0);
        LocalDateTime out = LocalDateTime.of(2026, 10, 29, 10, 30);
        req.setEngineerId(7L);
        req.setVisitDate(visitDate);
        req.setCheckInAt(in);
        req.setCheckOutAt(out);
        req.setWorkNotes("Cleaned fibre ferrule");
        req.setResolutionNotes("BER back to zero");

        assertEquals(7L, req.getEngineerId());
        assertEquals(visitDate, req.getVisitDate());
        assertEquals(in, req.getCheckInAt());
        assertEquals(out, req.getCheckOutAt());
        assertEquals("Cleaned fibre ferrule", req.getWorkNotes());
        assertEquals("BER back to zero", req.getResolutionNotes());
    }

    @Test
    @DisplayName("Verify ServiceVisitResponse maps accurately from ServiceVisit entity")
    void shouldMapServiceVisitResponse() {
        Incident incident = new Incident();
        incident.setId(101L);
        Engineer engineer = new Engineer();
        engineer.setId(42L);
        engineer.setName("Vikram Tech");

        LocalDate date = LocalDate.of(2026, 10, 29);
        ServiceVisit visit = new ServiceVisit(incident, engineer, date, "Splice completed");
        visit.setId(500L);
        LocalDateTime checkIn = LocalDateTime.of(2026, 10, 29, 14, 0);
        LocalDateTime checkOut = LocalDateTime.of(2026, 10, 29, 15, 45);
        visit.setCheckInAt(checkIn);
        visit.setCheckOutAt(checkOut);
        visit.setResolutionNotes("Core alignment successful");
        LocalDateTime now = LocalDateTime.now();
        visit.setCreatedAt(now);

        ServiceVisitResponse response = new ServiceVisitResponse(visit);

        assertEquals(500L, response.getId());
        assertEquals(101L, response.getIncidentId());
        assertEquals(42L, response.getEngineerId());
        assertEquals("Vikram Tech", response.getEngineerName());
        assertEquals(date, response.getVisitDate());
        assertEquals(checkIn, response.getCheckInAt());
        assertEquals(checkOut, response.getCheckOutAt());
        assertEquals("Splice completed", response.getWorkNotes());
        assertEquals("Core alignment successful", response.getResolutionNotes());
        assertEquals(now, response.getCreatedAt());
    }
}
