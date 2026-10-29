package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentComment;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.visit.entity.ServiceVisit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class IncidentCommentDtoTest {

    @Test
    @DisplayName("Verify CreateIncidentCommentRequest getters and setters")
    void shouldTestCreateIncidentCommentRequest() {
        CreateIncidentCommentRequest req = new CreateIncidentCommentRequest();
        req.setText("Diagnosed faulty SFP+ module");

        assertEquals("Diagnosed faulty SFP+ module", req.getText());

        CreateIncidentCommentRequest req2 = new CreateIncidentCommentRequest("Direct constructor");
        assertEquals("Direct constructor", req2.getText());
    }

    @Test
    @DisplayName("Verify IncidentCommentResponse from entity and constructor")
    void shouldTestIncidentCommentResponse() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 10L);

        LocalDateTime now = LocalDateTime.now();
        IncidentComment comment = new IncidentComment(incident, "admin", "Hardware replaced", now);
        ReflectionTestUtils.setField(comment, "id", 100L);

        IncidentCommentResponse response = new IncidentCommentResponse(comment);
        assertEquals(100L, response.getId());
        assertEquals(10L, response.getIncidentId());
        assertEquals("admin", response.getAuthor());
        assertEquals("Hardware replaced", response.getText());
        assertEquals(now, response.getCreatedAt());

        IncidentCommentResponse explicit = new IncidentCommentResponse(1L, 2L, "user1", "Text", now);
        assertEquals(1L, explicit.getId());
        assertEquals(2L, explicit.getIncidentId());
        assertEquals("user1", explicit.getAuthor());
        assertEquals("Text", explicit.getText());
        assertEquals(now, explicit.getCreatedAt());

        IncidentCommentResponse empty = new IncidentCommentResponse();
        assertNull(empty.getId());
    }

    @Test
    @DisplayName("Verify TimelineItemResponse from history, comments, visits and comparison")
    void shouldTestTimelineItemResponse() {
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 29, 9, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 29, 10, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 10, 29, 11, 0);

        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);

        IncidentHistory history = new IncidentHistory(incident, "CREATED", "Ticket opened", "dispatcher");
        ReflectionTestUtils.setField(history, "id", 1L);
        history.setTimestamp(t1);

        IncidentComment comment = new IncidentComment(incident, "tech_alok", "Patch cord replaced", t2);
        ReflectionTestUtils.setField(comment, "id", 2L);

        Engineer eng = new Engineer();
        eng.setName("Ravi Kumar");
        ServiceVisit visit = new ServiceVisit(incident, eng, LocalDate.now(), "Splice box aligned");
        ReflectionTestUtils.setField(visit, "id", 3L);
        visit.setCheckInAt(t3);
        visit.setResolutionNotes("Optical power stabilized");

        TimelineItemResponse item1 = TimelineItemResponse.fromHistory(history);
        assertEquals("history-1", item1.getId());
        assertEquals("HISTORY", item1.getItemType());
        assertEquals("CREATED", item1.getAction());
        assertEquals("dispatcher", item1.getActor());
        assertEquals("Ticket opened", item1.getContent());
        assertEquals(t1, item1.getTimestamp());

        TimelineItemResponse item2 = TimelineItemResponse.fromComment(comment);
        assertEquals("comment-2", item2.getId());
        assertEquals("COMMENT", item2.getItemType());
        assertEquals("COMMENT", item2.getAction());
        assertEquals("tech_alok", item2.getActor());
        assertEquals("Patch cord replaced", item2.getContent());
        assertEquals(t2, item2.getTimestamp());

        TimelineItemResponse item3 = TimelineItemResponse.fromServiceVisit(visit);
        assertEquals("visit-3", item3.getId());
        assertEquals("VISIT", item3.getItemType());
        assertEquals("SERVICE_VISIT", item3.getAction());
        assertEquals("Ravi Kumar", item3.getActor());
        assertEquals("Splice box aligned", item3.getContent());
        assertEquals("Optical power stabilized", item3.getDetails());
        assertEquals(t3, item3.getTimestamp());

        // CompareTo sorting
        assertTrue(item1.compareTo(item2) < 0);
        assertTrue(item3.compareTo(item2) > 0);
        assertEquals(0, item1.compareTo(item1));

        TimelineItemResponse nullTime = new TimelineItemResponse();
        assertTrue(nullTime.compareTo(item1) < 0);
        assertTrue(item1.compareTo(nullTime) > 0);
    }
}
