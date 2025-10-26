package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationErrorTest {

    @Test
    void testNoArgsConstructor() {
        ValidationError error = new ValidationError();

        assertNotNull(error);
        assertNull(error.getLine());
        assertNull(error.getColumn());
        assertNull(error.getMessage());
        assertNull(error.getSeverity());
        assertNull(error.getPath());
    }

    @Test
    void testTwoArgsConstructor() {
        String message = "Required field 'uuid' is missing";
        String severity = "ERROR";

        ValidationError error = new ValidationError(message, severity);

        assertNotNull(error);
        assertEquals(message, error.getMessage());
        assertEquals(severity, error.getSeverity());
        assertNull(error.getLine());
        assertNull(error.getColumn());
        assertNull(error.getPath());
    }

    @Test
    void testTwoArgsConstructorWithNullValues() {
        ValidationError error = new ValidationError(null, null);

        assertNotNull(error);
        assertNull(error.getMessage());
        assertNull(error.getSeverity());
    }

    @Test
    void testFiveArgsConstructor() {
        Integer line = 42;
        Integer column = 15;
        String message = "Invalid value for field 'title'";
        String severity = "WARNING";
        String path = "/catalog/metadata/title";

        ValidationError error = new ValidationError(line, column, message, severity, path);

        assertNotNull(error);
        assertEquals(line, error.getLine());
        assertEquals(column, error.getColumn());
        assertEquals(message, error.getMessage());
        assertEquals(severity, error.getSeverity());
        assertEquals(path, error.getPath());
    }

    @Test
    void testFiveArgsConstructorWithNullValues() {
        ValidationError error = new ValidationError(null, null, null, null, null);

        assertNotNull(error);
        assertNull(error.getLine());
        assertNull(error.getColumn());
        assertNull(error.getMessage());
        assertNull(error.getSeverity());
        assertNull(error.getPath());
    }

    @Test
    void testSetLine() {
        ValidationError error = new ValidationError();

        error.setLine(100);
        assertEquals(100, error.getLine());
    }

    @Test
    void testSetLineToNull() {
        ValidationError error = new ValidationError(10, 5, "Error", "ERROR", "/path");

        error.setLine(null);
        assertNull(error.getLine());
    }

    @Test
    void testSetColumn() {
        ValidationError error = new ValidationError();

        error.setColumn(25);
        assertEquals(25, error.getColumn());
    }

    @Test
    void testSetColumnToNull() {
        ValidationError error = new ValidationError(10, 5, "Error", "ERROR", "/path");

        error.setColumn(null);
        assertNull(error.getColumn());
    }

    @Test
    void testSetMessage() {
        ValidationError error = new ValidationError();
        String message = "Validation failed: schema constraint violation";

        error.setMessage(message);
        assertEquals(message, error.getMessage());
    }

    @Test
    void testSetMessageToNull() {
        ValidationError error = new ValidationError("Error", "ERROR");

        error.setMessage(null);
        assertNull(error.getMessage());
    }

    @Test
    void testSetSeverity() {
        ValidationError error = new ValidationError();

        error.setSeverity("CRITICAL");
        assertEquals("CRITICAL", error.getSeverity());
    }

    @Test
    void testSetSeverityToNull() {
        ValidationError error = new ValidationError("Error", "ERROR");

        error.setSeverity(null);
        assertNull(error.getSeverity());
    }

    @Test
    void testSetPath() {
        ValidationError error = new ValidationError();
        String path = "/system-security-plan/control-implementation";

        error.setPath(path);
        assertEquals(path, error.getPath());
    }

    @Test
    void testSetPathToNull() {
        ValidationError error = new ValidationError(1, 1, "Error", "ERROR", "/path");

        error.setPath(null);
        assertNull(error.getPath());
    }

    @Test
    void testMultipleSeverityLevels() {
        String[] severities = {"ERROR", "WARNING", "INFO", "CRITICAL", "FATAL"};

        for (String severity : severities) {
            ValidationError error = new ValidationError("Message", severity);
            assertEquals(severity, error.getSeverity());
        }
    }

    @Test
    void testLineAndColumnZero() {
        ValidationError error = new ValidationError(0, 0, "Error at start", "ERROR", "/");

        assertEquals(0, error.getLine());
        assertEquals(0, error.getColumn());
    }

    @Test
    void testLineAndColumnLargeNumbers() {
        ValidationError error = new ValidationError(999999, 999999, "Error", "ERROR", "/path");

        assertEquals(999999, error.getLine());
        assertEquals(999999, error.getColumn());
    }

    @Test
    void testEmptyMessageAndSeverity() {
        ValidationError error = new ValidationError("", "");

        assertEquals("", error.getMessage());
        assertEquals("", error.getSeverity());
    }

    @Test
    void testLongMessage() {
        String longMessage = "This is a very long error message that describes in detail what went wrong " +
                "during the validation process. It includes information about the expected value, " +
                "the actual value found, and suggestions for how to fix the issue. " +
                "This type of message might appear when validating complex OSCAL documents.";

        ValidationError error = new ValidationError(longMessage, "ERROR");

        assertEquals(longMessage, error.getMessage());
        assertTrue(error.getMessage().length() > 200);
    }

    @Test
    void testComplexPath() {
        String complexPath = "/catalog/groups[0]/controls[5]/parts[2]/props[1]/value";

        ValidationError error = new ValidationError(null, null, "Invalid prop", "WARNING", complexPath);

        assertEquals(complexPath, error.getPath());
        assertTrue(error.getPath().contains("["));
    }

    @Test
    void testJsonPointerPath() {
        String jsonPointerPath = "/system-security-plan/metadata/roles/0/id";

        ValidationError error = new ValidationError(null, null, "Duplicate role ID", "ERROR", jsonPointerPath);

        assertEquals(jsonPointerPath, error.getPath());
        assertTrue(error.getPath().startsWith("/"));
    }

    @Test
    void testXPathStylePath() {
        String xpathPath = "//catalog/metadata/title";

        ValidationError error = new ValidationError(null, null, "Missing title", "ERROR", xpathPath);

        assertEquals(xpathPath, error.getPath());
        assertTrue(error.getPath().startsWith("//"));
    }

    @Test
    void testUpdateAllFields() {
        ValidationError error = new ValidationError();

        error.setLine(50);
        error.setColumn(10);
        error.setMessage("Updated message");
        error.setSeverity("INFO");
        error.setPath("/new/path");

        assertEquals(50, error.getLine());
        assertEquals(10, error.getColumn());
        assertEquals("Updated message", error.getMessage());
        assertEquals("INFO", error.getSeverity());
        assertEquals("/new/path", error.getPath());
    }

    @Test
    void testClearAllFields() {
        ValidationError error = new ValidationError(10, 20, "Error", "ERROR", "/path");

        error.setLine(null);
        error.setColumn(null);
        error.setMessage(null);
        error.setSeverity(null);
        error.setPath(null);

        assertNull(error.getLine());
        assertNull(error.getColumn());
        assertNull(error.getMessage());
        assertNull(error.getSeverity());
        assertNull(error.getPath());
    }

    @Test
    void testSpecialCharactersInMessage() {
        String messageWithSpecialChars = "Error: \"value\" must match pattern [a-z]+, found '123'";

        ValidationError error = new ValidationError(messageWithSpecialChars, "ERROR");

        assertEquals(messageWithSpecialChars, error.getMessage());
        assertTrue(error.getMessage().contains("\""));
        assertTrue(error.getMessage().contains("'"));
    }

    @Test
    void testNegativeLineAndColumn() {
        // Edge case: negative values (shouldn't happen but test setter)
        ValidationError error = new ValidationError();

        error.setLine(-1);
        error.setColumn(-1);

        assertEquals(-1, error.getLine());
        assertEquals(-1, error.getColumn());
    }

    @Test
    void testSchemaValidationScenario() {
        ValidationError error = new ValidationError(
                15,
                42,
                "Element 'metadata' is missing required child element 'last-modified'",
                "ERROR",
                "/catalog/metadata"
        );

        assertTrue(error.getLine() > 0);
        assertTrue(error.getColumn() > 0);
        assertNotNull(error.getMessage());
        assertEquals("ERROR", error.getSeverity());
        assertNotNull(error.getPath());
    }
}
