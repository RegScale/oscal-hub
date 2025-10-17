package gov.nist.oscal.tools.api.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ValidationRuleSeverity {
    ERROR("error"),
    WARNING("warning"),
    INFO("info");

    private final String value;

    ValidationRuleSeverity(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static ValidationRuleSeverity fromString(String value) {
        if (value == null) {
            return null;
        }
        for (ValidationRuleSeverity severity : values()) {
            if (severity.value.equalsIgnoreCase(value)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown validation rule severity: " + value);
    }
}
