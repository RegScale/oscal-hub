package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileResolutionResultTest {

    @Test
    void testSuccessConstructor() {
        boolean success = true;
        String resolvedCatalog = "{\"catalog\": {...}}";
        Integer controlCount = 150;

        ProfileResolutionResult result = new ProfileResolutionResult(success, resolvedCatalog, controlCount);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(resolvedCatalog, result.getResolvedCatalog());
        assertEquals(controlCount, result.getControlCount());
        assertNull(result.getError()); // Error should be null in success case
    }

    @Test
    void testSuccessConstructorWithNullCatalog() {
        boolean success = true;
        String resolvedCatalog = null;
        Integer controlCount = 0;

        ProfileResolutionResult result = new ProfileResolutionResult(success, resolvedCatalog, controlCount);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNull(result.getResolvedCatalog());
        assertEquals(0, result.getControlCount());
        assertNull(result.getError());
    }

    @Test
    void testSuccessConstructorWithNullControlCount() {
        boolean success = true;
        String resolvedCatalog = "{\"catalog\": {...}}";
        Integer controlCount = null;

        ProfileResolutionResult result = new ProfileResolutionResult(success, resolvedCatalog, controlCount);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(resolvedCatalog, result.getResolvedCatalog());
        assertNull(result.getControlCount());
    }

    @Test
    void testErrorConstructor() {
        boolean success = false;
        String error = "Failed to resolve profile: Invalid profile reference";

        ProfileResolutionResult result = new ProfileResolutionResult(success, error);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(error, result.getError());
        assertNull(result.getResolvedCatalog()); // Catalog should be null in error case
        assertNull(result.getControlCount()); // Control count should be null in error case
    }

    @Test
    void testErrorConstructorWithNullError() {
        boolean success = false;
        String error = null;

        ProfileResolutionResult result = new ProfileResolutionResult(success, error);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getError());
    }

    @Test
    void testSetSuccess() {
        ProfileResolutionResult result = new ProfileResolutionResult(false, "Error");

        result.setSuccess(true);
        assertTrue(result.isSuccess());

        result.setSuccess(false);
        assertFalse(result.isSuccess());
    }

    @Test
    void testSetResolvedCatalog() {
        ProfileResolutionResult result = new ProfileResolutionResult(true, null, 0);
        String catalog = "{\"catalog\": {\"uuid\": \"123\"}}";

        result.setResolvedCatalog(catalog);

        assertEquals(catalog, result.getResolvedCatalog());
    }

    @Test
    void testSetResolvedCatalogToNull() {
        ProfileResolutionResult result = new ProfileResolutionResult(true, "{...}", 10);

        result.setResolvedCatalog(null);

        assertNull(result.getResolvedCatalog());
    }

    @Test
    void testSetError() {
        ProfileResolutionResult result = new ProfileResolutionResult(false, null);
        String error = "Profile not found";

        result.setError(error);

        assertEquals(error, result.getError());
    }

    @Test
    void testSetErrorToNull() {
        ProfileResolutionResult result = new ProfileResolutionResult(false, "Error");

        result.setError(null);

        assertNull(result.getError());
    }

    @Test
    void testSetControlCount() {
        ProfileResolutionResult result = new ProfileResolutionResult(true, "{...}", null);

        result.setControlCount(250);

        assertEquals(250, result.getControlCount());
    }

    @Test
    void testSetControlCountToNull() {
        ProfileResolutionResult result = new ProfileResolutionResult(true, "{...}", 100);

        result.setControlCount(null);

        assertNull(result.getControlCount());
    }

    @Test
    void testSuccessScenarioWithLargeCatalog() {
        String largeCatalog = "{\"catalog\": {\"uuid\": \"abc-123\", \"controls\": [...]}}".repeat(100);
        Integer largeControlCount = 10000;

        ProfileResolutionResult result = new ProfileResolutionResult(true, largeCatalog, largeControlCount);

        assertTrue(result.isSuccess());
        assertEquals(largeCatalog, result.getResolvedCatalog());
        assertEquals(10000, result.getControlCount());
    }

    @Test
    void testSuccessScenarioWithZeroControls() {
        ProfileResolutionResult result = new ProfileResolutionResult(true, "{\"catalog\": {}}", 0);

        assertTrue(result.isSuccess());
        assertNotNull(result.getResolvedCatalog());
        assertEquals(0, result.getControlCount());
    }

    @Test
    void testErrorScenarioWithDetailedMessage() {
        String detailedError = "Profile resolution failed: Unable to resolve import from 'http://example.com/profile'. " +
                "Reason: Connection timeout after 30 seconds. Please check network connectivity and try again.";

        ProfileResolutionResult result = new ProfileResolutionResult(false, detailedError);

        assertFalse(result.isSuccess());
        assertEquals(detailedError, result.getError());
        assertTrue(result.getError().contains("timeout"));
    }

    @Test
    void testConvertSuccessToError() {
        ProfileResolutionResult result = new ProfileResolutionResult(true, "{...}", 50);

        // Convert to error
        result.setSuccess(false);
        result.setError("Validation failed after resolution");
        result.setResolvedCatalog(null);
        result.setControlCount(null);

        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
        assertNull(result.getResolvedCatalog());
        assertNull(result.getControlCount());
    }

    @Test
    void testConvertErrorToSuccess() {
        ProfileResolutionResult result = new ProfileResolutionResult(false, "Error");

        // Convert to success
        result.setSuccess(true);
        result.setResolvedCatalog("{\"catalog\": {...}}");
        result.setControlCount(75);
        result.setError(null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getResolvedCatalog());
        assertEquals(75, result.getControlCount());
        assertNull(result.getError());
    }

    @Test
    void testMultipleControlCounts() {
        Integer[] controlCounts = {0, 1, 10, 100, 1000, 10000, Integer.MAX_VALUE};

        for (Integer count : controlCounts) {
            ProfileResolutionResult result = new ProfileResolutionResult(true, "{...}", count);
            assertEquals(count, result.getControlCount());
        }
    }

    @Test
    void testEmptyCatalogString() {
        ProfileResolutionResult result = new ProfileResolutionResult(true, "", 0);

        assertTrue(result.isSuccess());
        assertEquals("", result.getResolvedCatalog());
        assertNotNull(result.getResolvedCatalog());
    }

    @Test
    void testEmptyErrorString() {
        ProfileResolutionResult result = new ProfileResolutionResult(false, "");

        assertFalse(result.isSuccess());
        assertEquals("", result.getError());
        assertNotNull(result.getError());
    }

    @Test
    void testSuccessWithXmlCatalog() {
        String xmlCatalog = "<?xml version=\"1.0\"?><catalog xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\">...</catalog>";

        ProfileResolutionResult result = new ProfileResolutionResult(true, xmlCatalog, 42);

        assertTrue(result.isSuccess());
        assertTrue(result.getResolvedCatalog().startsWith("<?xml"));
        assertEquals(42, result.getControlCount());
    }

    @Test
    void testSuccessWithYamlCatalog() {
        String yamlCatalog = "catalog:\n  uuid: abc-123\n  metadata:\n    title: My Catalog\n  controls:\n    - id: ac-1";

        ProfileResolutionResult result = new ProfileResolutionResult(true, yamlCatalog, 1);

        assertTrue(result.isSuccess());
        assertTrue(result.getResolvedCatalog().contains("catalog:"));
        assertEquals(1, result.getControlCount());
    }

    @Test
    void testNegativeControlCount() {
        // Edge case: negative control count (shouldn't happen but test setter)
        ProfileResolutionResult result = new ProfileResolutionResult(true, "{...}", 10);

        result.setControlCount(-1);

        assertEquals(-1, result.getControlCount());
    }
}
