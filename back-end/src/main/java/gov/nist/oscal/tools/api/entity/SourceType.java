package gov.nist.oscal.tools.api.entity;

/**
 * Identifies which builder produced a library item. Soft pointer — the source
 * row may be deleted independently and the library item survives.
 */
public enum SourceType {
    CATALOG,
    PROFILE,
    SSP,
    AP,
    AR,
    POAM,
    COMPONENT_DEFINITION
}
