-- ============================================================================
-- V1.15: Performance Indexes
-- ============================================================================
-- This migration adds indexes to improve query performance on frequently
-- accessed columns. These indexes address the N+1 query patterns and
-- slow list/search operations identified in the performance audit.
-- ============================================================================

-- Artifact table indexes
-- Used for: sorting by date, filtering by organization, visibility-based queries
CREATE INDEX IF NOT EXISTS idx_artifact_created_at ON artifacts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_artifact_updated_at ON artifacts(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_artifact_organization_id ON artifacts(organization_id);
CREATE INDEX IF NOT EXISTS idx_artifact_visibility ON artifacts(visibility);
CREATE INDEX IF NOT EXISTS idx_artifact_created_by ON artifacts(created_by);
CREATE INDEX IF NOT EXISTS idx_artifact_download_count ON artifacts(download_count DESC);
CREATE INDEX IF NOT EXISTS idx_artifact_view_count ON artifacts(view_count DESC);

-- ArtifactVersion table indexes
-- Used for: version lookups, ordering versions by date
CREATE INDEX IF NOT EXISTS idx_artifact_version_artifact_id ON artifact_versions(artifact_id);
CREATE INDEX IF NOT EXISTS idx_artifact_version_created_at ON artifact_versions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_artifact_version_uploaded_by ON artifact_versions(uploaded_by);

-- ArtifactComment table indexes
-- Used for: threaded comments, filtering by artifact
CREATE INDEX IF NOT EXISTS idx_artifact_comment_artifact_id ON artifact_comments(artifact_id);
CREATE INDEX IF NOT EXISTS idx_artifact_comment_parent_id ON artifact_comments(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_artifact_comment_user_id ON artifact_comments(user_id);
CREATE INDEX IF NOT EXISTS idx_artifact_comment_created_at ON artifact_comments(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_artifact_comment_deleted ON artifact_comments(deleted);

-- ArtifactRating table indexes
-- Used for: user rating lookups, aggregating ratings by artifact
CREATE INDEX IF NOT EXISTS idx_artifact_rating_artifact_id ON artifact_ratings(artifact_id);
CREATE INDEX IF NOT EXISTS idx_artifact_rating_user_id ON artifact_ratings(user_id);

-- LibraryItem table indexes
-- Used for: sorting, type filtering, popularity queries
CREATE INDEX IF NOT EXISTS idx_library_item_created_at ON library_items(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_library_item_updated_at ON library_items(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_library_item_oscal_type ON library_items(oscal_type);
CREATE INDEX IF NOT EXISTS idx_library_item_created_by ON library_items(created_by);
CREATE INDEX IF NOT EXISTS idx_library_item_download_count ON library_items(download_count DESC);
CREATE INDEX IF NOT EXISTS idx_library_item_view_count ON library_items(view_count DESC);

-- LibraryVersion table indexes
CREATE INDEX IF NOT EXISTS idx_library_version_library_item_id ON library_versions(library_item_id);
CREATE INDEX IF NOT EXISTS idx_library_version_created_at ON library_versions(created_at DESC);

-- LibraryComment table indexes
CREATE INDEX IF NOT EXISTS idx_library_comment_library_item_id ON library_comments(library_item_id);
CREATE INDEX IF NOT EXISTS idx_library_comment_parent_id ON library_comments(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_library_comment_user_id ON library_comments(user_id);
CREATE INDEX IF NOT EXISTS idx_library_comment_created_at ON library_comments(created_at DESC);

-- LibraryRating table indexes
CREATE INDEX IF NOT EXISTS idx_library_rating_library_item_id ON library_ratings(library_item_id);
CREATE INDEX IF NOT EXISTS idx_library_rating_user_id ON library_ratings(user_id);

-- OperationHistory table indexes
-- Used for: user history, date filtering, model type analytics
CREATE INDEX IF NOT EXISTS idx_operation_history_user_id ON operation_history(user_id);
CREATE INDEX IF NOT EXISTS idx_operation_history_timestamp ON operation_history(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_operation_history_model_type ON operation_history(model_type);
CREATE INDEX IF NOT EXISTS idx_operation_history_operation_type ON operation_history(operation_type);
CREATE INDEX IF NOT EXISTS idx_operation_history_status ON operation_history(status);

-- Authorization table indexes
-- Used for: SSP lookups, status filtering, date ordering
CREATE INDEX IF NOT EXISTS idx_authorization_ssp_item_id ON authorizations(ssp_item_id);
CREATE INDEX IF NOT EXISTS idx_authorization_sar_item_id ON authorizations(sar_item_id);
CREATE INDEX IF NOT EXISTS idx_authorization_template_id ON authorizations(template_id);
CREATE INDEX IF NOT EXISTS idx_authorization_authorized_by ON authorizations(authorized_by);
CREATE INDEX IF NOT EXISTS idx_authorization_created_at ON authorizations(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_authorization_authorized_at ON authorizations(authorized_at DESC);
CREATE INDEX IF NOT EXISTS idx_authorization_date_authorized ON authorizations(date_authorized);
CREATE INDEX IF NOT EXISTS idx_authorization_date_expired ON authorizations(date_expired);

-- AuthorizationTemplate table indexes
CREATE INDEX IF NOT EXISTS idx_authorization_template_created_by ON authorization_templates(created_by);
CREATE INDEX IF NOT EXISTS idx_authorization_template_created_at ON authorization_templates(created_at DESC);

-- ConditionOfApproval table indexes
CREATE INDEX IF NOT EXISTS idx_condition_approval_authorization_id ON conditions_of_approval(authorization_id);
CREATE INDEX IF NOT EXISTS idx_condition_approval_status ON conditions_of_approval(status);
CREATE INDEX IF NOT EXISTS idx_condition_approval_due_date ON conditions_of_approval(due_date);

-- AuditEvent table indexes (additional to existing)
-- Note: Some indexes may already exist, IF NOT EXISTS handles this safely
CREATE INDEX IF NOT EXISTS idx_audit_event_category ON audit_events(category);
CREATE INDEX IF NOT EXISTS idx_audit_event_risk_level ON audit_events(risk_level);
CREATE INDEX IF NOT EXISTS idx_audit_event_created_at ON audit_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_event_user_agent ON audit_events(user_agent);
CREATE INDEX IF NOT EXISTS idx_audit_event_session_id ON audit_events(session_id);

-- User table indexes
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_created_at ON users(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_last_login ON users(last_login DESC);
CREATE INDEX IF NOT EXISTS idx_user_enabled ON users(enabled);
CREATE INDEX IF NOT EXISTS idx_user_global_role ON users(global_role);

-- Organization table indexes
CREATE INDEX IF NOT EXISTS idx_organization_created_at ON organizations(created_at DESC);

-- OrganizationMembership table indexes
-- Used for: user organization lookups, role-based access control
CREATE INDEX IF NOT EXISTS idx_org_membership_user_id ON organization_memberships(user_id);
CREATE INDEX IF NOT EXISTS idx_org_membership_org_id ON organization_memberships(organization_id);
CREATE INDEX IF NOT EXISTS idx_org_membership_status ON organization_memberships(status);
CREATE INDEX IF NOT EXISTS idx_org_membership_role ON organization_memberships(role);

-- UserAccessRequest table indexes
CREATE INDEX IF NOT EXISTS idx_user_access_request_user_id ON user_access_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_user_access_request_org_id ON user_access_requests(organization_id);
CREATE INDEX IF NOT EXISTS idx_user_access_request_status ON user_access_requests(status);
CREATE INDEX IF NOT EXISTS idx_user_access_request_created_at ON user_access_requests(created_at DESC);

-- SavedFile table indexes
CREATE INDEX IF NOT EXISTS idx_saved_file_user_id ON saved_files(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_file_created_at ON saved_files(created_at DESC);

-- ComponentDefinition table indexes
CREATE INDEX IF NOT EXISTS idx_component_def_user_id ON component_definitions(user_id);
CREATE INDEX IF NOT EXISTS idx_component_def_created_at ON component_definitions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_component_def_component_type ON component_definitions(component_type);

-- ReusableElement table indexes
CREATE INDEX IF NOT EXISTS idx_reusable_element_user_id ON reusable_elements(user_id);
CREATE INDEX IF NOT EXISTS idx_reusable_element_created_at ON reusable_elements(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reusable_element_element_type ON reusable_elements(element_type);

-- CustomValidationRule table indexes
CREATE INDEX IF NOT EXISTS idx_custom_validation_rule_user_id ON custom_validation_rules(user_id);
CREATE INDEX IF NOT EXISTS idx_custom_validation_rule_model_type ON custom_validation_rules(model_type);
CREATE INDEX IF NOT EXISTS idx_custom_validation_rule_enabled ON custom_validation_rules(enabled);

-- Composite indexes for common query patterns
-- These indexes support multi-column WHERE clauses and ordering

-- Artifact: visibility + organization combo (common access control query)
CREATE INDEX IF NOT EXISTS idx_artifact_visibility_org ON artifacts(visibility, organization_id);

-- Artifact: user + visibility + date (my artifacts sorted by date)
CREATE INDEX IF NOT EXISTS idx_artifact_user_visibility_date ON artifacts(created_by, visibility, updated_at DESC);

-- AuditEvent: username + timestamp (user activity queries)
CREATE INDEX IF NOT EXISTS idx_audit_event_username_timestamp ON audit_events(username, timestamp DESC);

-- AuditEvent: category + risk_level (filtered audit queries)
CREATE INDEX IF NOT EXISTS idx_audit_event_category_risk ON audit_events(category, risk_level);

-- OperationHistory: user + timestamp (user history with date filter)
CREATE INDEX IF NOT EXISTS idx_operation_history_user_timestamp ON operation_history(user_id, timestamp DESC);

-- OrganizationMembership: user + status (active membership lookup)
CREATE INDEX IF NOT EXISTS idx_org_membership_user_status ON organization_memberships(user_id, status);

-- ============================================================================
-- Note: Some tables may not exist depending on the application state.
-- PostgreSQL will silently skip CREATE INDEX IF NOT EXISTS for missing tables.
-- If you encounter errors, run migrations in order from V1.0.
-- ============================================================================
