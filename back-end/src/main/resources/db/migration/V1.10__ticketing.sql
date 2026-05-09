-- V1.10 — Ticketing system: tickets, threaded comments, file attachments.
-- See docs/superpowers/specs/2026-05-08-ticketing-system-design.md.

CREATE TABLE IF NOT EXISTS tickets (
    id              BIGSERIAL PRIMARY KEY,
    reporter_id     BIGINT NOT NULL REFERENCES users(id),
    type            VARCHAR(16) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    priority        VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMP(6) WITHOUT TIME ZONE NULL,
    CONSTRAINT ck_tickets_type CHECK (type IN ('BUG','FEATURE')),
    CONSTRAINT ck_tickets_status CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED','WONT_FIX','DUPLICATE')),
    CONSTRAINT ck_tickets_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);

CREATE INDEX IF NOT EXISTS idx_tickets_reporter_status ON tickets(reporter_id, status);
CREATE INDEX IF NOT EXISTS idx_tickets_status_created ON tickets(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tickets_type_status ON tickets(type, status);
CREATE INDEX IF NOT EXISTS idx_tickets_updated ON tickets(updated_at DESC);

CREATE TABLE IF NOT EXISTS ticket_comments (
    id                  BIGSERIAL PRIMARY KEY,
    ticket_id           BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id           BIGINT NOT NULL REFERENCES users(id),
    body                TEXT NOT NULL,
    is_status_change    BOOLEAN NOT NULL DEFAULT false,
    old_status          VARCHAR(16) NULL,
    new_status          VARCHAR(16) NULL,
    created_at          TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ticket_comments_ticket_created ON ticket_comments(ticket_id, created_at);

CREATE TABLE IF NOT EXISTS ticket_attachments (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    comment_id      BIGINT NULL REFERENCES ticket_comments(id) ON DELETE CASCADE,
    uploader_id     BIGINT NOT NULL REFERENCES users(id),
    filename        VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    storage_path    VARCHAR(512) NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ticket_attachments_ticket ON ticket_attachments(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_attachments_comment ON ticket_attachments(comment_id);
