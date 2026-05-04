-- V1.17: Add Artifact Tables
-- Date: 2026-02-18
-- Description: Creates all tables for the artifact/template system

-- ============================================================================
-- 1. Artifact Tags Table (must be created before artifacts due to FK)
-- ============================================================================
CREATE TABLE IF NOT EXISTS artifact_tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_artifact_tag_name ON artifact_tags(name);

-- ============================================================================
-- 2. Main Artifacts Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS artifacts (
    id BIGSERIAL PRIMARY KEY,
    artifact_id VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    organization_id BIGINT REFERENCES organizations(id) ON DELETE SET NULL,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_version_id BIGINT, -- FK added after artifact_versions table created
    extracted_variables TEXT,
    download_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT artifacts_visibility_check CHECK (visibility IN ('PRIVATE', 'ORGANIZATION', 'PUBLIC'))
);

-- Indexes for artifacts (matching V1.15 expectations)
CREATE INDEX IF NOT EXISTS idx_artifact_created_at ON artifacts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_artifact_updated_at ON artifacts(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_artifact_organization_id ON artifacts(organization_id);
CREATE INDEX IF NOT EXISTS idx_artifact_visibility ON artifacts(visibility);
CREATE INDEX IF NOT EXISTS idx_artifact_created_by ON artifacts(created_by);
CREATE INDEX IF NOT EXISTS idx_artifact_download_count ON artifacts(download_count DESC);
CREATE INDEX IF NOT EXISTS idx_artifact_view_count ON artifacts(view_count DESC);
CREATE INDEX IF NOT EXISTS idx_artifact_visibility_org ON artifacts(visibility, organization_id);
CREATE INDEX IF NOT EXISTS idx_artifact_user_visibility_date ON artifacts(created_by, visibility, updated_at DESC);

-- ============================================================================
-- 3. Artifact Versions Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS artifact_versions (
    id BIGSERIAL PRIMARY KEY,
    version_id VARCHAR(100) NOT NULL UNIQUE,
    artifact_id BIGINT NOT NULL REFERENCES artifacts(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    content_size BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    uploaded_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_description VARCHAR(1000),
    title_snapshot VARCHAR(255),
    description_snapshot VARCHAR(2000),
    visibility_snapshot VARCHAR(20),
    extracted_variables_snapshot TEXT
);

CREATE INDEX IF NOT EXISTS idx_artifact_version_artifact ON artifact_versions(artifact_id);
CREATE INDEX IF NOT EXISTS idx_artifact_version_uploaded_at ON artifact_versions(uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_artifact_version_number ON artifact_versions(artifact_id, version_number DESC);

-- Add FK for current_version_id now that artifact_versions exists
ALTER TABLE artifacts
    ADD CONSTRAINT fk_artifact_current_version
    FOREIGN KEY (current_version_id)
    REFERENCES artifact_versions(id)
    ON DELETE SET NULL;

-- ============================================================================
-- 4. Artifact Tag Mapping (Join Table)
-- ============================================================================
CREATE TABLE IF NOT EXISTS artifact_tag_mapping (
    artifact_id BIGINT NOT NULL REFERENCES artifacts(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES artifact_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (artifact_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_artifact_tag_mapping_artifact ON artifact_tag_mapping(artifact_id);
CREATE INDEX IF NOT EXISTS idx_artifact_tag_mapping_tag ON artifact_tag_mapping(tag_id);

-- ============================================================================
-- 5. Artifact Comments Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS artifact_comments (
    id BIGSERIAL PRIMARY KEY,
    comment_id VARCHAR(100) NOT NULL UNIQUE,
    artifact_id BIGINT NOT NULL REFERENCES artifacts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_comment_id BIGINT REFERENCES artifact_comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_artifact_comment_artifact ON artifact_comments(artifact_id);
CREATE INDEX IF NOT EXISTS idx_artifact_comment_user ON artifact_comments(user_id);
CREATE INDEX IF NOT EXISTS idx_artifact_comment_parent ON artifact_comments(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_artifact_comment_created ON artifact_comments(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_artifact_comment_deleted ON artifact_comments(deleted);

-- ============================================================================
-- 6. Artifact Ratings Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS artifact_ratings (
    id BIGSERIAL PRIMARY KEY,
    artifact_id BIGINT NOT NULL REFERENCES artifacts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (artifact_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_artifact_rating_artifact ON artifact_ratings(artifact_id);
CREATE INDEX IF NOT EXISTS idx_artifact_rating_user ON artifact_ratings(user_id);
