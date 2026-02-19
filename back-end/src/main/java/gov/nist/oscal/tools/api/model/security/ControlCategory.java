package gov.nist.oscal.tools.api.model.security;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * SOC 2 Trust Service Criteria categories.
 */
@Schema(description = "SOC 2 Trust Service Criteria category")
public enum ControlCategory {
    @Schema(description = "Logical and Physical Access Controls")
    CC6("CC6", "Logical and Physical Access Controls"),

    @Schema(description = "System Operations")
    CC7("CC7", "System Operations"),

    @Schema(description = "Change Management")
    CC8("CC8", "Change Management"),

    @Schema(description = "Risk Mitigation")
    CC9("CC9", "Risk Mitigation"),

    @Schema(description = "Data Protection")
    DATA_PROTECTION("DATA", "Data Protection"),

    @Schema(description = "Audit and Monitoring")
    AUDIT("AUDIT", "Audit and Monitoring");

    private final String code;
    private final String displayName;

    ControlCategory(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ControlCategory fromCode(String code) {
        for (ControlCategory category : values()) {
            if (category.code.equalsIgnoreCase(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown control category: " + code);
    }
}
