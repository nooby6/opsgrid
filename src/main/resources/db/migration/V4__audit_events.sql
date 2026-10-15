CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_id VARCHAR(120) NOT NULL CHECK (length(trim(actor_id)) > 0),
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    before_state JSONB,
    after_state JSONB
);

CREATE INDEX idx_audit_events_entity_time
    ON audit_events(entity_type, entity_id, occurred_at DESC);

CREATE INDEX idx_audit_events_actor_time
    ON audit_events(actor_id, occurred_at DESC);
