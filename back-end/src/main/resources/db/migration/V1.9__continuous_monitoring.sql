-- V1.9 — Continuous Monitoring snapshots, POAM items, and reconciliations.
-- See docs/superpowers/specs/2026-05-06-conmon-and-documents-design.md.

CREATE TABLE IF NOT EXISTS conmon_snapshots (
    id                     BIGSERIAL PRIMARY KEY,
    authorization_id       BIGINT NOT NULL REFERENCES authorizations(id) ON DELETE CASCADE,
    uploaded_by            BIGINT NOT NULL REFERENCES users(id),
    uploaded_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_format          VARCHAR(32) NOT NULL,
    original_filename      VARCHAR(512) NOT NULL,
    file_storage_path      VARCHAR(1024) NOT NULL,
    oscal_uuid             VARCHAR(64),
    oscal_version          VARCHAR(16),
    metadata_title         VARCHAR(512),
    metadata_last_modified TIMESTAMP,
    summary_open_count     INT NOT NULL DEFAULT 0,
    summary_closed_count   INT NOT NULL DEFAULT 0,
    summary_unknown_count  INT NOT NULL DEFAULT 0,
    notes                  TEXT,
    CONSTRAINT ck_conmon_snapshots_format CHECK (source_format IN
        ('OSCAL_JSON','OSCAL_XML','OSCAL_YAML','FEDRAMP_XLSX'))
);
CREATE INDEX IF NOT EXISTS idx_conmon_snapshots_auth
    ON conmon_snapshots (authorization_id, uploaded_at DESC);

CREATE TABLE IF NOT EXISTS conmon_poam_items (
    id                          BIGSERIAL PRIMARY KEY,
    snapshot_id                 BIGINT NOT NULL REFERENCES conmon_snapshots(id) ON DELETE CASCADE,
    external_id                 VARCHAR(128) NOT NULL,
    title                       VARCHAR(1024) NOT NULL,
    description                 TEXT,
    status                      VARCHAR(16) NOT NULL,
    raw_status                  VARCHAR(64),
    severity                    VARCHAR(16),
    weakness_source             VARCHAR(256),
    scheduled_completion_date   DATE,
    actual_completion_date      DATE,
    point_of_contact            VARCHAR(256),
    risk_rating                 VARCHAR(64),
    extra_props_json            TEXT,
    CONSTRAINT ck_conmon_poam_items_status CHECK (status IN ('OPEN','CLOSED','UNKNOWN')),
    CONSTRAINT ck_conmon_poam_items_severity CHECK (severity IS NULL OR severity IN
        ('LOW','MODERATE','HIGH','CRITICAL'))
);
CREATE INDEX IF NOT EXISTS idx_conmon_poam_items_snap_status
    ON conmon_poam_items (snapshot_id, status);
CREATE INDEX IF NOT EXISTS idx_conmon_poam_items_snap_extid
    ON conmon_poam_items (snapshot_id, external_id);

CREATE TABLE IF NOT EXISTS conmon_reconciliations (
    id                   BIGSERIAL PRIMARY KEY,
    snapshot_id          BIGINT NOT NULL REFERENCES conmon_snapshots(id) ON DELETE CASCADE UNIQUE,
    previous_snapshot_id BIGINT NOT NULL REFERENCES conmon_snapshots(id),
    new_count            INT NOT NULL DEFAULT 0,
    closed_count         INT NOT NULL DEFAULT 0,
    reopened_count       INT NOT NULL DEFAULT 0,
    still_open_count     INT NOT NULL DEFAULT 0,
    removed_count        INT NOT NULL DEFAULT 0,
    changed_count        INT NOT NULL DEFAULT 0
);
