package gov.nist.oscal.tools.api.entity;

/**
 * Three-tier access control for library items.
 * PRIVATE: only the creator
 * ORGANIZATION: anyone in the same organization
 * PUBLIC: visible at the public catalog and via the public API
 */
public enum Visibility {
    PRIVATE,
    ORGANIZATION,
    PUBLIC
}
