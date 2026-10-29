package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.incident.entity.IncidentComment;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.visit.entity.ServiceVisit;

import java.time.LocalDateTime;

public class TimelineItemResponse implements Comparable<TimelineItemResponse> {

    private String id;
    private String itemType;
    private String action;
    private String actor;
    private String content;
    private String details;
    private LocalDateTime timestamp;

    public TimelineItemResponse() {
    }

    public TimelineItemResponse(
            String id,
            String itemType,
            String action,
            String actor,
            String content,
            String details,
            LocalDateTime timestamp) {
        this.id = id;
        this.itemType = itemType;
        this.action = action;
        this.actor = actor;
        this.content = content;
        this.details = details;
        this.timestamp = timestamp;
    }

    public static TimelineItemResponse fromHistory(IncidentHistory history) {
        return new TimelineItemResponse(
                "history-" + history.getId(),
                "HISTORY",
                history.getAction(),
                history.getPerformedBy() != null ? history.getPerformedBy() : "system",
                history.getDescription(),
                null,
                history.getTimestamp()
        );
    }

    public static TimelineItemResponse fromComment(IncidentComment comment) {
        return new TimelineItemResponse(
                "comment-" + comment.getId(),
                "COMMENT",
                "COMMENT",
                comment.getAuthor(),
                comment.getText(),
                null,
                comment.getCreatedAt()
        );
    }

    public static TimelineItemResponse fromServiceVisit(ServiceVisit visit) {
        String actor = visit.getEngineer() != null ? visit.getEngineer().getName() : "Technician";
        LocalDateTime time = visit.getCheckInAt() != null ? visit.getCheckInAt() : visit.getCreatedAt();
        return new TimelineItemResponse(
                "visit-" + visit.getId(),
                "VISIT",
                "SERVICE_VISIT",
                actor,
                visit.getWorkNotes(),
                visit.getResolutionNotes(),
                time
        );
    }

    @Override
    public int compareTo(TimelineItemResponse o) {
        if (this.timestamp == null && o.timestamp == null) return 0;
        if (this.timestamp == null) return -1;
        if (o.timestamp == null) return 1;
        return this.timestamp.compareTo(o.timestamp);
    }

    public String getId() {
        return id;
    }

    public String getItemType() {
        return itemType;
    }

    public String getAction() {
        return action;
    }

    public String getActor() {
        return actor;
    }

    public String getContent() {
        return content;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
