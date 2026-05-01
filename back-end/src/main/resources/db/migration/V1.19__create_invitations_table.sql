-- V1.19: Invitations for teammate onboarding by email
-- Description: Stores token-based invitations sent by org admins to teammates.

CREATE TABLE invitations (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    invited_by_user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,
    accepted_by_user_id BIGINT REFERENCES users(id)
);

CREATE INDEX idx_invitations_email ON invitations(email);
CREATE INDEX idx_invitations_org_status ON invitations(organization_id, status);
