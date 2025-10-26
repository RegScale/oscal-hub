package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRuleSeverityTest {

    @Test
    void testEnumValues() {
        ValidationRuleSeverity[] severities = ValidationRuleSeverity.values();
        assertEquals(3, severities.length);
    }

    @Test
    void testErrorSeverity() {
        ValidationRuleSeverity severity = ValidationRuleSeverity.ERROR;
        assertEquals("error", severity.getValue());
    }

    @Test
    void testWarningSeverity() {
        ValidationRuleSeverity severity = ValidationRuleSeverity.WARNING;
        assertEquals("warning", severity.getValue());
    }

    @Test
    void testInfoSeverity() {
        ValidationRuleSeverity severity = ValidationRuleSeverity.INFO;
        assertEquals("info", severity.getValue());
    }

    @Test
    void testFromStringWithError() {
        ValidationRuleSeverity severity = ValidationRuleSeverity.fromString("error");
        assertEquals(ValidationRuleSeverity.ERROR, severity);
    }

    @Test
    void testFromStringWithWarning() {
        ValidationRuleSeverity severity = ValidationRuleSeverity.fromString("warning");
        assertEquals(ValidationRuleSeverity.WARNING, severity);
    }

    @Test
    void testFromStringWithInfo() {
        ValidationRuleSeverity severity = ValidationRuleSeverity.fromString("info");
        assertEquals(ValidationRuleSeverity.INFO, severity);
    }

    @Test
    void testFromStringCaseInsensitive() {
        ValidationRuleSeverity severity1 = ValidationRuleSeverity.fromString("ERROR");
        assertEquals(ValidationRuleSeverity.ERROR, severity1);

        ValidationRuleSeverity severity2 = ValidationRuleSeverity.fromString("Warning");
        assertEquals(ValidationRuleSeverity.WARNING, severity2);

        ValidationRuleSeverity severity3 = ValidationRuleSeverity.fromString("INFO");
        assertEquals(ValidationRuleSeverity.INFO, severity3);
    }

    @Test
    void testFromStringWithNull() {
        ValidationRuleSeverity severity = ValidationRuleSeverity.fromString(null);
        assertNull(severity);
    }

    @Test
    void testFromStringWithInvalidValue() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ValidationRuleSeverity.fromString("critical");
        });

        assertEquals("Unknown validation rule severity: critical", exception.getMessage());
    }

    @Test
    void testFromStringWithEmptyString() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ValidationRuleSeverity.fromString("");
        });

        assertEquals("Unknown validation rule severity: ", exception.getMessage());
    }

    @Test
    void testFromStringWithWhitespace() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ValidationRuleSeverity.fromString("   ");
        });

        assertEquals("Unknown validation rule severity:    ", exception.getMessage());
    }

    @Test
    void testAllEnumValuesCanBeFoundByString() {
        for (ValidationRuleSeverity severity : ValidationRuleSeverity.values()) {
            ValidationRuleSeverity found = ValidationRuleSeverity.fromString(severity.getValue());
            assertEquals(severity, found);
        }
    }

    @Test
    void testGetValueReturnsCorrectString() {
        assertEquals("error", ValidationRuleSeverity.ERROR.getValue());
        assertEquals("warning", ValidationRuleSeverity.WARNING.getValue());
        assertEquals("info", ValidationRuleSeverity.INFO.getValue());
    }

    @Test
    void testFromStringWithMixedCase() {
        assertEquals(ValidationRuleSeverity.ERROR, ValidationRuleSeverity.fromString("ErRoR"));
        assertEquals(ValidationRuleSeverity.WARNING, ValidationRuleSeverity.fromString("wArNiNg"));
        assertEquals(ValidationRuleSeverity.INFO, ValidationRuleSeverity.fromString("InFo"));
    }

    @Test
    void testFromStringWithInvalidCasing() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ValidationRuleSeverity.fromString("CRITICAL");
        });

        assertTrue(exception.getMessage().contains("Unknown validation rule severity"));
    }

    @Test
    void testEnumOrdering() {
        ValidationRuleSeverity[] severities = ValidationRuleSeverity.values();
        assertEquals(ValidationRuleSeverity.ERROR, severities[0]);
        assertEquals(ValidationRuleSeverity.WARNING, severities[1]);
        assertEquals(ValidationRuleSeverity.INFO, severities[2]);
    }
}
