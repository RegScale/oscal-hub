-- V1.23: AI session events log + computed cost

ALTER TABLE ai_sessions
    ADD COLUMN events_json     TEXT,
    ADD COLUMN cost_usd_micros BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_ai_sessions_org_status_time
    ON ai_sessions(organization_id, status, started_at DESC);
