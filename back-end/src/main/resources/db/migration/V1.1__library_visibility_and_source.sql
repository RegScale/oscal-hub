-- V1.1__library_visibility_and_source.sql
-- Adds three-tier visibility, source-pointer linkage to builder rows,
-- and publish timestamps to library_items.

ALTER TABLE library_items
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    ADD COLUMN IF NOT EXISTS organization_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(40) NULL,
    ADD COLUMN IF NOT EXISTS source_id UUID NULL,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS last_published_at TIMESTAMP NULL;

ALTER TABLE library_items
    DROP CONSTRAINT IF EXISTS library_items_visibility_check;
ALTER TABLE library_items
    ADD CONSTRAINT library_items_visibility_check
    CHECK (visibility IN ('PRIVATE', 'ORGANIZATION', 'PUBLIC'));

ALTER TABLE library_items
    DROP CONSTRAINT IF EXISTS library_items_source_type_check;
ALTER TABLE library_items
    ADD CONSTRAINT library_items_source_type_check
    CHECK (source_type IS NULL OR source_type IN
        ('CATALOG','PROFILE','SSP','AP','AR','POAM','COMPONENT_DEFINITION'));

ALTER TABLE library_items
    DROP CONSTRAINT IF EXISTS library_items_organization_fk;
ALTER TABLE library_items
    ADD CONSTRAINT library_items_organization_fk
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_library_items_visibility_type
    ON library_items(visibility, oscal_type);
CREATE INDEX IF NOT EXISTS idx_library_items_visibility_org
    ON library_items(visibility, organization_id);
CREATE INDEX IF NOT EXISTS idx_library_items_source
    ON library_items(created_by, source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_library_items_search_fts
    ON library_items USING GIN (
        to_tsvector('english', coalesce(title,'') || ' ' || coalesce(description,''))
    );
