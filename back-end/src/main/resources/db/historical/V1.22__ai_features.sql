-- V1.22: AI features — per-org API key storage and session tracking

CREATE TABLE org_ai_settings (
    id                          BIGSERIAL PRIMARY KEY,
    organization_id             BIGINT NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    anthropic_key_encrypted     TEXT,
    anthropic_key_fingerprint   VARCHAR(32),
    default_model               VARCHAR(64) NOT NULL DEFAULT 'claude-opus-4-7',
    enabled                     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP
);

CREATE INDEX idx_org_ai_settings_org ON org_ai_settings(organization_id);

CREATE TABLE ai_sessions (
    id                  UUID PRIMARY KEY,
    organization_id     BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    wizard_kind         VARCHAR(32) NOT NULL,
    mode                VARCHAR(16) NOT NULL,
    model               VARCHAR(64) NOT NULL,
    input_summary       TEXT,
    status              VARCHAR(20) NOT NULL,
    tokens_in           INTEGER NOT NULL DEFAULT 0,
    tokens_out          INTEGER NOT NULL DEFAULT 0,
    error_code          VARCHAR(64),
    error_message       TEXT,
    started_at          TIMESTAMP NOT NULL,
    ended_at            TIMESTAMP
);

CREATE INDEX idx_ai_sessions_org_time ON ai_sessions(organization_id, started_at DESC);
CREATE INDEX idx_ai_sessions_user_time ON ai_sessions(user_id, started_at DESC);
