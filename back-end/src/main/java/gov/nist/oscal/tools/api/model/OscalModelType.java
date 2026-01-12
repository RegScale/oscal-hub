package gov.nist.oscal.tools.api.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OscalModelType {
    CATALOG("catalog"),
    PROFILE("profile"),
    COMPONENT_DEFINITION("component-definition"),
    SYSTEM_SECURITY_PLAN("system-security-plan"),
    ASSESSMENT_PLAN("assessment-plan"),
    ASSESSMENT_RESULTS("assessment-results"),
    PLAN_OF_ACTION_AND_MILESTONES("plan-of-action-and-milestones"),
    MAPPING_COLLECTION("mapping-collection");

    private final String value;

    OscalModelType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static OscalModelType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (OscalModelType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown OSCAL model type: " + value);
    }
}
