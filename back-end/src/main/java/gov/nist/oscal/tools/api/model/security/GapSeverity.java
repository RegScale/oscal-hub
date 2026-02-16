package gov.nist.oscal.tools.api.model.security;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Severity level for compliance gaps.
 */
@Schema(description = "Severity level for a compliance gap")
public enum GapSeverity {
    @Schema(description = "High severity - requires immediate attention")
    HIGH("High"),

    @Schema(description = "Medium severity - should be addressed soon")
    MEDIUM("Medium"),

    @Schema(description = "Low severity - can be addressed in future iterations")
    LOW("Low");

    private final String displayName;

    GapSeverity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
