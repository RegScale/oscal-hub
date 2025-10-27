-- V1.6: Rename azure_blob_path to storage_path in component_definitions
-- Date: 2025-10-26
-- Description: Refactors storage path column to be provider-agnostic (Azure/S3/Local)
--              This supports the factory pattern for multiple storage providers

-- PostgreSQL: Rename column from azure_blob_path to storage_path
-- For H2 database (dev/testing), this command also works
ALTER TABLE component_definitions
    RENAME COLUMN azure_blob_path TO storage_path;

-- Update any comments to reflect the generic nature
COMMENT ON COLUMN component_definitions.storage_path IS 'Cloud storage path (Azure Blob Storage, AWS S3, or local): build/{username}/{filename}';
