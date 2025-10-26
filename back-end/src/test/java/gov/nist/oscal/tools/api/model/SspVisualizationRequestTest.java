package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SspVisualizationRequestTest {

    @Test
    void testNoArgsConstructor() {
        SspVisualizationRequest request = new SspVisualizationRequest();

        assertNotNull(request);
        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testTwoArgsConstructor() {
        String content = "{\"system-security-plan\": {}}";
        OscalFormat format = OscalFormat.JSON;

        SspVisualizationRequest request = new SspVisualizationRequest(content, format);

        assertNotNull(request);
        assertEquals(content, request.getContent());
        assertEquals(format, request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testThreeArgsConstructor() {
        String content = "<system-security-plan></system-security-plan>";
        OscalFormat format = OscalFormat.XML;
        String fileName = "ssp.xml";

        SspVisualizationRequest request = new SspVisualizationRequest(content, format, fileName);

        assertNotNull(request);
        assertEquals(content, request.getContent());
        assertEquals(format, request.getFormat());
        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testSetContent() {
        SspVisualizationRequest request = new SspVisualizationRequest();
        assertNull(request.getContent());

        String content = "{\"system-security-plan\": {\"metadata\": {}}}";
        request.setContent(content);
        assertEquals(content, request.getContent());
    }

    @Test
    void testSetFormat() {
        SspVisualizationRequest request = new SspVisualizationRequest();
        assertNull(request.getFormat());

        request.setFormat(OscalFormat.JSON);
        assertEquals(OscalFormat.JSON, request.getFormat());

        request.setFormat(OscalFormat.XML);
        assertEquals(OscalFormat.XML, request.getFormat());

        request.setFormat(OscalFormat.YAML);
        assertEquals(OscalFormat.YAML, request.getFormat());
    }

    @Test
    void testSetFileName() {
        SspVisualizationRequest request = new SspVisualizationRequest();
        assertNull(request.getFileName());

        String fileName = "system-security-plan.json";
        request.setFileName(fileName);
        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testSetAllFieldsToNull() {
        SspVisualizationRequest request = new SspVisualizationRequest("{}", OscalFormat.JSON, "file.json");

        request.setContent(null);
        request.setFormat(null);
        request.setFileName(null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testWithJsonContent() {
        String jsonContent = "{\"system-security-plan\": {\"uuid\": \"12345\", \"metadata\": {\"title\": \"Test SSP\"}}}";

        SspVisualizationRequest request = new SspVisualizationRequest(jsonContent, OscalFormat.JSON, "test-ssp.json");

        assertEquals(jsonContent, request.getContent());
        assertEquals(OscalFormat.JSON, request.getFormat());
        assertEquals("test-ssp.json", request.getFileName());
    }

    @Test
    void testWithXmlContent() {
        String xmlContent = "<?xml version=\"1.0\"?><system-security-plan><uuid>12345</uuid></system-security-plan>";

        SspVisualizationRequest request = new SspVisualizationRequest(xmlContent, OscalFormat.XML, "test-ssp.xml");

        assertEquals(xmlContent, request.getContent());
        assertEquals(OscalFormat.XML, request.getFormat());
        assertEquals("test-ssp.xml", request.getFileName());
    }

    @Test
    void testWithYamlContent() {
        String yamlContent = "system-security-plan:\n  uuid: 12345\n  metadata:\n    title: Test SSP";

        SspVisualizationRequest request = new SspVisualizationRequest(yamlContent, OscalFormat.YAML, "test-ssp.yaml");

        assertEquals(yamlContent, request.getContent());
        assertEquals(OscalFormat.YAML, request.getFormat());
        assertEquals("test-ssp.yaml", request.getFileName());
    }

    @Test
    void testWithLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("{\"system-security-plan\": {\"data\": \"test\"}}");
        }

        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent(longContent.toString());

        assertTrue(request.getContent().length() > 1000);
    }

    @Test
    void testWithEmptyContent() {
        SspVisualizationRequest request = new SspVisualizationRequest("", OscalFormat.JSON);

        assertEquals("", request.getContent());
        assertEquals(OscalFormat.JSON, request.getFormat());
    }

    @Test
    void testWithEmptyFileName() {
        SspVisualizationRequest request = new SspVisualizationRequest("{}", OscalFormat.JSON, "");

        assertEquals("", request.getFileName());
    }

    @Test
    void testWithSpecialCharactersInFileName() {
        String fileName = "my-test_file (v1.2).json";
        SspVisualizationRequest request = new SspVisualizationRequest("{}", OscalFormat.JSON, fileName);

        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testWithPathInFileName() {
        String fileName = "/path/to/files/ssp.json";
        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setFileName(fileName);

        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testModifyAllFields() {
        SspVisualizationRequest request = new SspVisualizationRequest("{}", OscalFormat.JSON, "old.json");

        String newContent = "<system-security-plan></system-security-plan>";
        OscalFormat newFormat = OscalFormat.XML;
        String newFileName = "new.xml";

        request.setContent(newContent);
        request.setFormat(newFormat);
        request.setFileName(newFileName);

        assertEquals(newContent, request.getContent());
        assertEquals(newFormat, request.getFormat());
        assertEquals(newFileName, request.getFileName());
    }

    @Test
    void testConstructorWithNullValues() {
        SspVisualizationRequest request = new SspVisualizationRequest(null, null, null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testTwoArgsConstructorWithNulls() {
        SspVisualizationRequest request = new SspVisualizationRequest(null, null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }
}
