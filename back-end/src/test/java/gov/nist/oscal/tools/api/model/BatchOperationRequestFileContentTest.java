package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatchOperationRequestFileContentTest {

    @Test
    void testNoArgsConstructor() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();

        assertNotNull(fileContent);
        assertNull(fileContent.getFilename());
        assertNull(fileContent.getContent());
        assertNull(fileContent.getFormat());
    }

    @Test
    void testAllArgsConstructor() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent(
            "catalog.json",
            "{\"catalog\": {}}",
            OscalFormat.JSON
        );

        assertEquals("catalog.json", fileContent.getFilename());
        assertEquals("{\"catalog\": {}}", fileContent.getContent());
        assertEquals(OscalFormat.JSON, fileContent.getFormat());
    }

    @Test
    void testSetFilename() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
        fileContent.setFilename("profile.xml");
        assertEquals("profile.xml", fileContent.getFilename());
    }

    @Test
    void testSetContent() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
        fileContent.setContent("<profile></profile>");
        assertEquals("<profile></profile>", fileContent.getContent());
    }

    @Test
    void testSetFormat() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
        fileContent.setFormat(OscalFormat.YAML);
        assertEquals(OscalFormat.YAML, fileContent.getFormat());
    }

    @Test
    void testSetAllFields() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
        fileContent.setFilename("ssp.xml");
        fileContent.setContent("<system-security-plan></system-security-plan>");
        fileContent.setFormat(OscalFormat.XML);

        assertEquals("ssp.xml", fileContent.getFilename());
        assertEquals("<system-security-plan></system-security-plan>", fileContent.getContent());
        assertEquals(OscalFormat.XML, fileContent.getFormat());
    }

    @Test
    void testSetFieldsToNull() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent(
            "test.json",
            "{}",
            OscalFormat.JSON
        );
        fileContent.setFilename(null);
        fileContent.setContent(null);
        fileContent.setFormat(null);

        assertNull(fileContent.getFilename());
        assertNull(fileContent.getContent());
        assertNull(fileContent.getFormat());
    }

    @Test
    void testModifyAllFields() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();

        fileContent.setFilename("first.json");
        fileContent.setContent("{\"first\": true}");
        fileContent.setFormat(OscalFormat.JSON);

        fileContent.setFilename("second.xml");
        fileContent.setContent("<second></second>");
        fileContent.setFormat(OscalFormat.XML);

        assertEquals("second.xml", fileContent.getFilename());
        assertEquals("<second></second>", fileContent.getContent());
        assertEquals(OscalFormat.XML, fileContent.getFormat());
    }

    @Test
    void testWithEmptyStrings() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent(
            "",
            "",
            null
        );

        assertEquals("", fileContent.getFilename());
        assertEquals("", fileContent.getContent());
        assertNull(fileContent.getFormat());
    }

    @Test
    void testWithJsonFormat() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
        fileContent.setFilename("catalog.json");
        fileContent.setContent("{\"catalog\": {\"uuid\": \"123\"}}");
        fileContent.setFormat(OscalFormat.JSON);

        assertEquals("catalog.json", fileContent.getFilename());
        assertTrue(fileContent.getContent().contains("catalog"));
        assertEquals(OscalFormat.JSON, fileContent.getFormat());
    }

    @Test
    void testWithXmlFormat() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
        fileContent.setFilename("profile.xml");
        fileContent.setContent("<?xml version=\"1.0\"?><profile></profile>");
        fileContent.setFormat(OscalFormat.XML);

        assertEquals("profile.xml", fileContent.getFilename());
        assertTrue(fileContent.getContent().contains("xml"));
        assertEquals(OscalFormat.XML, fileContent.getFormat());
    }

    @Test
    void testWithYamlFormat() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
        fileContent.setFilename("ssp.yaml");
        fileContent.setContent("system-security-plan:\n  uuid: abc-123");
        fileContent.setFormat(OscalFormat.YAML);

        assertEquals("ssp.yaml", fileContent.getFilename());
        assertTrue(fileContent.getContent().contains("system-security-plan"));
        assertEquals(OscalFormat.YAML, fileContent.getFormat());
    }

    @Test
    void testWithLongFilename() {
        String longFilename = "very_long_filename_with_many_characters_" + "x".repeat(100) + ".json";
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
        fileContent.setFilename(longFilename);
        assertEquals(longFilename, fileContent.getFilename());
    }

    @Test
    void testWithLargeContent() {
        String largeContent = "{\"data\": \"" + "x".repeat(10000) + "\"}";
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
        fileContent.setContent(largeContent);
        assertEquals(largeContent, fileContent.getContent());
    }

    @Test
    void testWithSpecialCharactersInFilename() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
        fileContent.setFilename("file-name_v1.2.3.json");
        assertEquals("file-name_v1.2.3.json", fileContent.getFilename());
    }

    @Test
    void testConstructorAndSettersCombined() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent(
            "initial.json",
            "{}",
            OscalFormat.JSON
        );

        assertEquals("initial.json", fileContent.getFilename());
        assertEquals("{}", fileContent.getContent());
        assertEquals(OscalFormat.JSON, fileContent.getFormat());

        fileContent.setFilename("updated.xml");
        fileContent.setContent("<root></root>");
        fileContent.setFormat(OscalFormat.XML);

        assertEquals("updated.xml", fileContent.getFilename());
        assertEquals("<root></root>", fileContent.getContent());
        assertEquals(OscalFormat.XML, fileContent.getFormat());
    }

    @Test
    void testWithPathInFilename() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
        fileContent.setFilename("path/to/catalog.json");
        assertEquals("path/to/catalog.json", fileContent.getFilename());
    }

    @Test
    void testCompleteFileScenario() {
        BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent(
            "nist-800-53-catalog.json",
            "{\"catalog\": {\"uuid\": \"abc-123\", \"metadata\": {}}}",
            OscalFormat.JSON
        );

        assertNotNull(fileContent);
        assertTrue(fileContent.getFilename().endsWith(".json"));
        assertTrue(fileContent.getContent().contains("catalog"));
        assertEquals(OscalFormat.JSON, fileContent.getFormat());
    }

    @Test
    void testWithAllFormats() {
        OscalFormat[] formats = {OscalFormat.JSON, OscalFormat.XML, OscalFormat.YAML};

        for (OscalFormat format : formats) {
            BatchOperationRequest.FileContent fileContent = new BatchOperationRequest.FileContent();
            fileContent.setFormat(format);
            assertEquals(format, fileContent.getFormat());
        }
    }

    @Test
    void testMultipleInstances() {
        BatchOperationRequest.FileContent file1 = new BatchOperationRequest.FileContent(
            "file1.json",
            "{}",
            OscalFormat.JSON
        );
        BatchOperationRequest.FileContent file2 = new BatchOperationRequest.FileContent(
            "file2.xml",
            "<root></root>",
            OscalFormat.XML
        );

        assertNotEquals(file1.getFilename(), file2.getFilename());
        assertNotEquals(file1.getContent(), file2.getContent());
        assertNotEquals(file1.getFormat(), file2.getFormat());
    }
}
