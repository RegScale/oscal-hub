package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResultTest {

    @Test
    void testNoArgsConstructor() {
        ValidationResult result = new ValidationResult();

        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrors());
        assertNotNull(result.getWarnings());
        assertEquals(0, result.getErrors().size());
        assertEquals(0, result.getWarnings().size());
        assertNull(result.getModelType());
        assertNull(result.getFormat());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testSingleArgConstructor() {
        ValidationResult result = new ValidationResult(true);

        assertTrue(result.isValid());
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getErrors());
        assertNotNull(result.getWarnings());
    }

    @Test
    void testSetValid() {
        ValidationResult result = new ValidationResult();
        assertFalse(result.isValid());

        result.setValid(true);
        assertTrue(result.isValid());

        result.setValid(false);
        assertFalse(result.isValid());
    }

    @Test
    void testSetErrors() {
        ValidationResult result = new ValidationResult();
        ValidationError error1 = new ValidationError();
        error1.setMessage("Error 1");
        ValidationError error2 = new ValidationError();
        error2.setMessage("Error 2");

        List<ValidationError> errors = Arrays.asList(error1, error2);
        result.setErrors(errors);

        assertEquals(2, result.getErrors().size());
        assertEquals("Error 1", result.getErrors().get(0).getMessage());
    }

    @Test
    void testSetWarnings() {
        ValidationResult result = new ValidationResult();
        ValidationError warning1 = new ValidationError();
        warning1.setMessage("Warning 1");
        ValidationError warning2 = new ValidationError();
        warning2.setMessage("Warning 2");

        List<ValidationError> warnings = Arrays.asList(warning1, warning2);
        result.setWarnings(warnings);

        assertEquals(2, result.getWarnings().size());
        assertEquals("Warning 1", result.getWarnings().get(0).getMessage());
    }

    @Test
    void testSetModelType() {
        ValidationResult result = new ValidationResult();
        result.setModelType(OscalModelType.CATALOG);
        assertEquals(OscalModelType.CATALOG, result.getModelType());

        result.setModelType(OscalModelType.SYSTEM_SECURITY_PLAN);
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, result.getModelType());
    }

    @Test
    void testSetFormat() {
        ValidationResult result = new ValidationResult();
        result.setFormat(OscalFormat.JSON);
        assertEquals(OscalFormat.JSON, result.getFormat());

        result.setFormat(OscalFormat.XML);
        assertEquals(OscalFormat.XML, result.getFormat());
    }

    @Test
    void testSetTimestamp() {
        ValidationResult result = new ValidationResult();
        String customTimestamp = "2025-01-15T10:30:00Z";
        result.setTimestamp(customTimestamp);
        assertEquals(customTimestamp, result.getTimestamp());
    }

    @Test
    void testAddError() {
        ValidationResult result = new ValidationResult();
        assertEquals(0, result.getErrors().size());

        ValidationError error = new ValidationError();
        error.setMessage("Validation failed");
        result.addError(error);

        assertEquals(1, result.getErrors().size());
        assertEquals("Validation failed", result.getErrors().get(0).getMessage());
    }

    @Test
    void testAddWarning() {
        ValidationResult result = new ValidationResult();
        assertEquals(0, result.getWarnings().size());

        ValidationError warning = new ValidationError();
        warning.setMessage("Potential issue detected");
        result.addWarning(warning);

        assertEquals(1, result.getWarnings().size());
        assertEquals("Potential issue detected", result.getWarnings().get(0).getMessage());
    }

    @Test
    void testAddMultipleErrors() {
        ValidationResult result = new ValidationResult();

        for (int i = 1; i <= 5; i++) {
            ValidationError error = new ValidationError();
            error.setMessage("Error " + i);
            result.addError(error);
        }

        assertEquals(5, result.getErrors().size());
        assertEquals("Error 1", result.getErrors().get(0).getMessage());
        assertEquals("Error 5", result.getErrors().get(4).getMessage());
    }

    @Test
    void testAddMultipleWarnings() {
        ValidationResult result = new ValidationResult();

        for (int i = 1; i <= 3; i++) {
            ValidationError warning = new ValidationError();
            warning.setMessage("Warning " + i);
            result.addWarning(warning);
        }

        assertEquals(3, result.getWarnings().size());
        assertEquals("Warning 1", result.getWarnings().get(0).getMessage());
    }

    @Test
    void testValidResultWithNoErrors() {
        ValidationResult result = new ValidationResult(true);
        result.setModelType(OscalModelType.CATALOG);
        result.setFormat(OscalFormat.JSON);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
        assertEquals(0, result.getWarnings().size());
    }

    @Test
    void testInvalidResultWithErrors() {
        ValidationResult result = new ValidationResult(false);
        result.setModelType(OscalModelType.PROFILE);

        ValidationError error1 = new ValidationError();
        error1.setMessage("Missing required field");
        result.addError(error1);

        ValidationError error2 = new ValidationError();
        error2.setMessage("Invalid format");
        result.addError(error2);

        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
    }

    @Test
    void testResultWithWarningsButValid() {
        ValidationResult result = new ValidationResult(true);

        ValidationError warning = new ValidationError();
        warning.setMessage("Deprecated field used");
        result.addWarning(warning);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    void testSetAllFields() {
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        result.setModelType(OscalModelType.SYSTEM_SECURITY_PLAN);
        result.setFormat(OscalFormat.YAML);
        result.setTimestamp("2025-03-20T15:45:00Z");

        ValidationError error = new ValidationError();
        error.setMessage("Test error");
        result.addError(error);

        ValidationError warning = new ValidationError();
        warning.setMessage("Test warning");
        result.addWarning(warning);

        assertTrue(result.isValid());
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, result.getModelType());
        assertEquals(OscalFormat.YAML, result.getFormat());
        assertEquals("2025-03-20T15:45:00Z", result.getTimestamp());
        assertEquals(1, result.getErrors().size());
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    void testSetFieldsToNull() {
        ValidationResult result = new ValidationResult(true);
        result.setModelType(OscalModelType.CATALOG);
        result.setFormat(OscalFormat.JSON);

        result.setModelType(null);
        result.setFormat(null);
        result.setTimestamp(null);
        result.setErrors(null);
        result.setWarnings(null);

        assertNull(result.getModelType());
        assertNull(result.getFormat());
        assertNull(result.getTimestamp());
        assertNull(result.getErrors());
        assertNull(result.getWarnings());
    }

    @Test
    void testModifyAllFields() {
        ValidationResult result = new ValidationResult(false);
        result.setModelType(OscalModelType.CATALOG);
        result.setFormat(OscalFormat.JSON);

        result.setValid(true);
        result.setModelType(OscalModelType.PROFILE);
        result.setFormat(OscalFormat.XML);

        assertTrue(result.isValid());
        assertEquals(OscalModelType.PROFILE, result.getModelType());
        assertEquals(OscalFormat.XML, result.getFormat());
    }

    @Test
    void testEmptyErrorsAndWarnings() {
        ValidationResult result = new ValidationResult();
        result.setErrors(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        assertNotNull(result.getErrors());
        assertNotNull(result.getWarnings());
        assertEquals(0, result.getErrors().size());
        assertEquals(0, result.getWarnings().size());
    }

    @Test
    void testWithAllOscalModelTypes() {
        OscalModelType[] types = OscalModelType.values();

        for (OscalModelType type : types) {
            ValidationResult result = new ValidationResult();
            result.setModelType(type);
            assertEquals(type, result.getModelType());
        }
    }

    @Test
    void testWithAllOscalFormats() {
        OscalFormat[] formats = OscalFormat.values();

        for (OscalFormat format : formats) {
            ValidationResult result = new ValidationResult();
            result.setFormat(format);
            assertEquals(format, result.getFormat());
        }
    }

    @Test
    void testCompleteValidationScenario() {
        ValidationResult result = new ValidationResult(false);
        result.setModelType(OscalModelType.SYSTEM_SECURITY_PLAN);
        result.setFormat(OscalFormat.JSON);

        ValidationError error1 = new ValidationError();
        error1.setMessage("Control AC-1 is missing");
        error1.setSeverity("ERROR");
        result.addError(error1);

        ValidationError error2 = new ValidationError();
        error2.setMessage("Invalid UUID format");
        error2.setSeverity("ERROR");
        result.addError(error2);

        ValidationError warning1 = new ValidationError();
        warning1.setMessage("Control description is empty");
        warning1.setSeverity("WARNING");
        result.addWarning(warning1);

        assertFalse(result.isValid());
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, result.getModelType());
        assertEquals(OscalFormat.JSON, result.getFormat());
        assertEquals(2, result.getErrors().size());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("AC-1"));
    }

    @Test
    void testTimestampIsSet() {
        ValidationResult result = new ValidationResult();
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp().length() > 0);
    }
}
