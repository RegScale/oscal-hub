package gov.nist.oscal.tools.api.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ValidationRuleType {
    REQUIRED_FIELD("required-field"),
    PATTERN_MATCH("pattern-match"),
    ALLOWED_VALUES("allowed-values"),
    CARDINALITY("cardinality"),
    CROSS_FIELD("cross-field"),
    ID_REFERENCE("id-reference"),
    DATA_TYPE("data-type"),
    CUSTOM("custom");

    private final String value;

    ValidationRuleType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static ValidationRuleType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (ValidationRuleType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown validation rule type: " + value);
    }
}
