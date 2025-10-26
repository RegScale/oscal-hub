package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConversionResultTest {

    @Test
    void testSuccessConstructor() {
        ConversionResult result = new ConversionResult(
            true,
            "{\"catalog\": {}}",
            OscalFormat.JSON,
            OscalFormat.XML
        );

        assertTrue(result.isSuccess());
        assertEquals("{\"catalog\": {}}", result.getContent());
        assertNull(result.getError());
        assertEquals(OscalFormat.JSON, result.getFromFormat());
        assertEquals(OscalFormat.XML, result.getToFormat());
    }

    @Test
    void testErrorConstructor() {
        ConversionResult result = new ConversionResult(
            false,
            "Conversion failed: Invalid format",
            OscalFormat.XML,
            OscalFormat.JSON,
            true
        );

        assertFalse(result.isSuccess());
        assertEquals("Conversion failed: Invalid format", result.getError());
        assertNull(result.getContent());
        assertEquals(OscalFormat.XML, result.getFromFormat());
        assertEquals(OscalFormat.JSON, result.getToFormat());
    }

    @Test
    void testSetSuccess() {
        ConversionResult result = new ConversionResult(
            true,
            "content",
            OscalFormat.JSON,
            OscalFormat.YAML
        );

        result.setSuccess(false);
        assertFalse(result.isSuccess());
    }

    @Test
    void testSetContent() {
        ConversionResult result = new ConversionResult(
            true,
            "original content",
            OscalFormat.JSON,
            OscalFormat.XML
        );

        result.setContent("updated content");
        assertEquals("updated content", result.getContent());
    }

    @Test
    void testSetError() {
        ConversionResult result = new ConversionResult(
            false,
            "original error",
            OscalFormat.XML,
            OscalFormat.JSON,
            true
        );

        result.setError("updated error");
        assertEquals("updated error", result.getError());
    }

    @Test
    void testSetFromFormat() {
        ConversionResult result = new ConversionResult(
            true,
            "content",
            OscalFormat.JSON,
            OscalFormat.XML
        );

        result.setFromFormat(OscalFormat.YAML);
        assertEquals(OscalFormat.YAML, result.getFromFormat());
    }

    @Test
    void testSetToFormat() {
        ConversionResult result = new ConversionResult(
            true,
            "content",
            OscalFormat.JSON,
            OscalFormat.XML
        );

        result.setToFormat(OscalFormat.YAML);
        assertEquals(OscalFormat.YAML, result.getToFormat());
    }

    @Test
    void testSuccessfulConversionWithEmptyContent() {
        ConversionResult result = new ConversionResult(
            true,
            "",
            OscalFormat.JSON,
            OscalFormat.XML
        );

        assertTrue(result.isSuccess());
        assertEquals("", result.getContent());
        assertNull(result.getError());
    }

    @Test
    void testFailedConversionWithEmptyError() {
        ConversionResult result = new ConversionResult(
            false,
            "",
            OscalFormat.XML,
            OscalFormat.JSON,
            true
        );

        assertFalse(result.isSuccess());
        assertEquals("", result.getError());
        assertNull(result.getContent());
    }

    @Test
    void testConversionJsonToXml() {
        String jsonContent = "{\"catalog\": {\"uuid\": \"550e8400-e29b-41d4-a716-446655440000\"}}";
        ConversionResult result = new ConversionResult(
            true,
            jsonContent,
            OscalFormat.JSON,
            OscalFormat.XML
        );

        assertTrue(result.isSuccess());
        assertEquals(jsonContent, result.getContent());
        assertEquals(OscalFormat.JSON, result.getFromFormat());
        assertEquals(OscalFormat.XML, result.getToFormat());
    }

    @Test
    void testConversionXmlToJson() {
        String xmlContent = "<?xml version=\"1.0\"?><catalog/>";
        ConversionResult result = new ConversionResult(
            true,
            xmlContent,
            OscalFormat.XML,
            OscalFormat.JSON
        );

        assertTrue(result.isSuccess());
        assertEquals(xmlContent, result.getContent());
        assertEquals(OscalFormat.XML, result.getFromFormat());
        assertEquals(OscalFormat.JSON, result.getToFormat());
    }

    @Test
    void testConversionYamlToJson() {
        String yamlContent = "catalog:\n  uuid: 550e8400-e29b-41d4-a716-446655440000";
        ConversionResult result = new ConversionResult(
            true,
            yamlContent,
            OscalFormat.YAML,
            OscalFormat.JSON
        );

        assertTrue(result.isSuccess());
        assertEquals(yamlContent, result.getContent());
        assertEquals(OscalFormat.YAML, result.getFromFormat());
        assertEquals(OscalFormat.JSON, result.getToFormat());
    }

    @Test
    void testConversionWithLargeContent() {
        String largeContent = "A".repeat(100000);
        ConversionResult result = new ConversionResult(
            true,
            largeContent,
            OscalFormat.JSON,
            OscalFormat.XML
        );

        assertTrue(result.isSuccess());
        assertEquals(largeContent, result.getContent());
        assertEquals(100000, result.getContent().length());
    }

    @Test
    void testConversionWithSpecialCharacters() {
        String specialContent = "Content with special chars: <>\"'&\n\t";
        ConversionResult result = new ConversionResult(
            true,
            specialContent,
            OscalFormat.JSON,
            OscalFormat.XML
        );

        assertTrue(result.isSuccess());
        assertEquals(specialContent, result.getContent());
    }

    @Test
    void testFailedConversionWithDetailedError() {
        String detailedError = "Conversion failed at line 42: Unexpected token '<' at position 123. " +
                             "Expected valid JSON structure but found malformed XML.";
        ConversionResult result = new ConversionResult(
            false,
            detailedError,
            OscalFormat.JSON,
            OscalFormat.XML,
            true
        );

        assertFalse(result.isSuccess());
        assertEquals(detailedError, result.getError());
    }

    @Test
    void testModifyAllFields() {
        ConversionResult result = new ConversionResult(
            true,
            "original content",
            OscalFormat.JSON,
            OscalFormat.XML
        );

        result.setSuccess(false);
        result.setContent(null);
        result.setError("new error");
        result.setFromFormat(OscalFormat.YAML);
        result.setToFormat(OscalFormat.JSON);

        assertFalse(result.isSuccess());
        assertNull(result.getContent());
        assertEquals("new error", result.getError());
        assertEquals(OscalFormat.YAML, result.getFromFormat());
        assertEquals(OscalFormat.JSON, result.getToFormat());
    }

    @Test
    void testSetAllFieldsToNull() {
        ConversionResult result = new ConversionResult(
            true,
            "content",
            OscalFormat.JSON,
            OscalFormat.XML
        );

        result.setContent(null);
        result.setError(null);
        result.setFromFormat(null);
        result.setToFormat(null);

        assertNull(result.getContent());
        assertNull(result.getError());
        assertNull(result.getFromFormat());
        assertNull(result.getToFormat());
    }

    @Test
    void testConversionWithNullContent() {
        ConversionResult result = new ConversionResult(
            true,
            null,
            OscalFormat.JSON,
            OscalFormat.XML
        );

        assertTrue(result.isSuccess());
        assertNull(result.getContent());
    }

    @Test
    void testConversionWithNullError() {
        ConversionResult result = new ConversionResult(
            false,
            null,
            OscalFormat.XML,
            OscalFormat.JSON,
            true
        );

        assertFalse(result.isSuccess());
        assertNull(result.getError());
    }

    @Test
    void testCompleteSuccessfulConversionScenario() {
        String jsonCatalog = "{\n" +
                           "  \"catalog\": {\n" +
                           "    \"uuid\": \"550e8400-e29b-41d4-a716-446655440000\",\n" +
                           "    \"metadata\": {\n" +
                           "      \"title\": \"Sample Catalog\",\n" +
                           "      \"version\": \"1.0\"\n" +
                           "    }\n" +
                           "  }\n" +
                           "}";

        ConversionResult result = new ConversionResult(
            true,
            jsonCatalog,
            OscalFormat.JSON,
            OscalFormat.XML
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().contains("550e8400-e29b-41d4-a716-446655440000"));
        assertTrue(result.getContent().contains("Sample Catalog"));
        assertNull(result.getError());
        assertEquals(OscalFormat.JSON, result.getFromFormat());
        assertEquals(OscalFormat.XML, result.getToFormat());
    }

    @Test
    void testCompleteFailedConversionScenario() {
        ConversionResult result = new ConversionResult(
            false,
            "Schema validation failed: Missing required field 'uuid' in catalog metadata",
            OscalFormat.JSON,
            OscalFormat.XML,
            true
        );

        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Schema validation failed"));
        assertTrue(result.getError().contains("uuid"));
        assertNull(result.getContent());
        assertEquals(OscalFormat.JSON, result.getFromFormat());
        assertEquals(OscalFormat.XML, result.getToFormat());
    }

    @Test
    void testSwitchFromSuccessToError() {
        ConversionResult result = new ConversionResult(
            true,
            "valid content",
            OscalFormat.JSON,
            OscalFormat.XML
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());

        // Simulate conversion failure
        result.setSuccess(false);
        result.setContent(null);
        result.setError("Conversion failed during processing");

        assertFalse(result.isSuccess());
        assertNull(result.getContent());
        assertEquals("Conversion failed during processing", result.getError());
    }

    @Test
    void testAllFormatCombinations() {
        OscalFormat[] formats = {OscalFormat.JSON, OscalFormat.XML, OscalFormat.YAML};

        for (OscalFormat from : formats) {
            for (OscalFormat to : formats) {
                ConversionResult result = new ConversionResult(
                    true,
                    "content",
                    from,
                    to
                );

                assertEquals(from, result.getFromFormat());
                assertEquals(to, result.getToFormat());
            }
        }
    }
}
