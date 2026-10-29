-- V8: Add notifications table for SLA alert engine

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_incident FOREIGN KEY (incident_id) REFERENCES incident (id) ON DELETE CASCADE,
    CONSTRAINT uq_notifications_incident_type UNIQUE (incident_id, type)
);

CREATE INDEX idx_notifications_incident_id ON notifications (incident_id);
CREATE INDEX idx_notifications_is_read_created_at ON notifications (is_read, created_at DESC);
