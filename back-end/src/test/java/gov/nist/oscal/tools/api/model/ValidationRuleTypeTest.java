package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRuleTypeTest {

    @Test
    void testEnumValues() {
        ValidationRuleType[] types = ValidationRuleType.values();
        assertEquals(8, types.length);
    }

    @Test
    void testRequiredFieldType() {
        ValidationRuleType type = ValidationRuleType.REQUIRED_FIELD;
        assertEquals("required-field", type.getValue());
    }

    @Test
    void testPatternMatchType() {
        ValidationRuleType type = ValidationRuleType.PATTERN_MATCH;
        assertEquals("pattern-match", type.getValue());
    }

    @Test
    void testAllowedValuesType() {
        ValidationRuleType type = ValidationRuleType.ALLOWED_VALUES;
        assertEquals("allowed-values", type.getValue());
    }

    @Test
    void testCardinalityType() {
        ValidationRuleType type = ValidationRuleType.CARDINALITY;
        assertEquals("cardinality", type.getValue());
    }

    @Test
    void testCrossFieldType() {
        ValidationRuleType type = ValidationRuleType.CROSS_FIELD;
        assertEquals("cross-field", type.getValue());
    }

    @Test
    void testIdReferenceType() {
        ValidationRuleType type = ValidationRuleType.ID_REFERENCE;
        assertEquals("id-reference", type.getValue());
    }

    @Test
    void testDataTypeType() {
        ValidationRuleType type = ValidationRuleType.DATA_TYPE;
        assertEquals("data-type", type.getValue());
    }

    @Test
    void testCustomType() {
        ValidationRuleType type = ValidationRuleType.CUSTOM;
        assertEquals("custom", type.getValue());
    }

    @Test
    void testFromStringWithRequiredField() {
        ValidationRuleType type = ValidationRuleType.fromString("required-field");
        assertEquals(ValidationRuleType.REQUIRED_FIELD, type);
    }

    @Test
    void testFromStringWithPatternMatch() {
        ValidationRuleType type = ValidationRuleType.fromString("pattern-match");
        assertEquals(ValidationRuleType.PATTERN_MATCH, type);
    }

    @Test
    void testFromStringWithAllowedValues() {
        ValidationRuleType type = ValidationRuleType.fromString("allowed-values");
        assertEquals(ValidationRuleType.ALLOWED_VALUES, type);
    }

    @Test
    void testFromStringWithCardinality() {
        ValidationRuleType type = ValidationRuleType.fromString("cardinality");
        assertEquals(ValidationRuleType.CARDINALITY, type);
    }

    @Test
    void testFromStringWithCrossField() {
        ValidationRuleType type = ValidationRuleType.fromString("cross-field");
        assertEquals(ValidationRuleType.CROSS_FIELD, type);
    }

    @Test
    void testFromStringWithIdReference() {
        ValidationRuleType type = ValidationRuleType.fromString("id-reference");
        assertEquals(ValidationRuleType.ID_REFERENCE, type);
    }

    @Test
    void testFromStringWithDataType() {
        ValidationRuleType type = ValidationRuleType.fromString("data-type");
        assertEquals(ValidationRuleType.DATA_TYPE, type);
    }

    @Test
    void testFromStringWithCustom() {
        ValidationRuleType type = ValidationRuleType.fromString("custom");
        assertEquals(ValidationRuleType.CUSTOM, type);
    }

    @Test
    void testFromStringCaseInsensitive() {
        ValidationRuleType type1 = ValidationRuleType.fromString("REQUIRED-FIELD");
        assertEquals(ValidationRuleType.REQUIRED_FIELD, type1);

        ValidationRuleType type2 = ValidationRuleType.fromString("Pattern-Match");
        assertEquals(ValidationRuleType.PATTERN_MATCH, type2);

        ValidationRuleType type3 = ValidationRuleType.fromString("CUSTOM");
        assertEquals(ValidationRuleType.CUSTOM, type3);
    }

    @Test
    void testFromStringWithNull() {
        ValidationRuleType type = ValidationRuleType.fromString(null);
        assertNull(type);
    }

    @Test
    void testFromStringWithInvalidValue() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ValidationRuleType.fromString("invalid-type");
        });

        assertEquals("Unknown validation rule type: invalid-type", exception.getMessage());
    }

    @Test
    void testFromStringWithEmptyString() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ValidationRuleType.fromString("");
        });

        assertEquals("Unknown validation rule type: ", exception.getMessage());
    }

    @Test
    void testFromStringWithWhitespace() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ValidationRuleType.fromString("   ");
        });

        assertEquals("Unknown validation rule type:    ", exception.getMessage());
    }

    @Test
    void testAllEnumValuesCanBeFoundByString() {
        for (ValidationRuleType type : ValidationRuleType.values()) {
            ValidationRuleType found = ValidationRuleType.fromString(type.getValue());
            assertEquals(type, found);
        }
    }

    @Test
    void testGetValueReturnsCorrectString() {
        assertEquals("required-field", ValidationRuleType.REQUIRED_FIELD.getValue());
        assertEquals("pattern-match", ValidationRuleType.PATTERN_MATCH.getValue());
        assertEquals("allowed-values", ValidationRuleType.ALLOWED_VALUES.getValue());
        assertEquals("cardinality", ValidationRuleType.CARDINALITY.getValue());
        assertEquals("cross-field", ValidationRuleType.CROSS_FIELD.getValue());
        assertEquals("id-reference", ValidationRuleType.ID_REFERENCE.getValue());
        assertEquals("data-type", ValidationRuleType.DATA_TYPE.getValue());
        assertEquals("custom", ValidationRuleType.CUSTOM.getValue());
    }
}
