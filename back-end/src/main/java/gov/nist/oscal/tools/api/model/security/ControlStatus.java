package gov.nist.oscal.tools.api.model.security;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Implementation status for SOC 2 controls.
 */
@Schema(description = "Implementation status for a SOC 2 control")
public enum ControlStatus {
    @Schema(description = "Control is fully implemented")
    IMPLEMENTED("Implemented"),

    @Schema(description = "Control is partially implemented")
    PARTIAL("Partial"),

    @Schema(description = "Control is not implemented - gap exists")
    GAP("Gap");

    private final String displayName;

    ControlStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
