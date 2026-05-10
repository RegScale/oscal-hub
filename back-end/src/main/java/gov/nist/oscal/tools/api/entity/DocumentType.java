package gov.nist.oscal.tools.api.entity;

/**
 * Document categories attached to an authorization. The enum value is stored
 * as a VARCHAR via @Enumerated(EnumType.STRING). The 16 values match the
 * V1.8 CHECK constraint and the spec's package-completeness checklist.
 */
public enum DocumentType {
    VULNERABILITY_SCAN,
    PENETRATION_TEST,
    ASSET_INVENTORY,
    SSP,
    SAR,
    CONFIGURATION_BASELINE,
    CONTINGENCY_PLAN,
    INCIDENT_RESPONSE_PLAN,
    AUDIT_REPORT,
    AUTHORIZATION_LETTER,
    CHANGE_NOTICE_TICKET,
    RISK_ASSESSMENT,
    BUSINESS_CONTINUITY_PLAN,
    DISASTER_RECOVERY_PLAN,
    BUSINESS_IMPACT_ASSESSMENT,
    OTHER
}
