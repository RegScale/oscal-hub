package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SarVisualizationRequestTest {

    @Test
    void testNoArgsConstructor() {
        SarVisualizationRequest request = new SarVisualizationRequest();

        assertNotNull(request);
        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testTwoArgsConstructor() {
        String content = "{\"assessment-results\": {}}";
        OscalFormat format = OscalFormat.JSON;

        SarVisualizationRequest request = new SarVisualizationRequest(content, format);

        assertNotNull(request);
        assertEquals(content, request.getContent());
        assertEquals(format, request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testThreeArgsConstructor() {
        String content = "<assessment-results></assessment-results>";
        OscalFormat format = OscalFormat.XML;
        String fileName = "sar.xml";

        SarVisualizationRequest request = new SarVisualizationRequest(content, format, fileName);

        assertNotNull(request);
        assertEquals(content, request.getContent());
        assertEquals(format, request.getFormat());
        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testSetContent() {
        SarVisualizationRequest request = new SarVisualizationRequest();
        assertNull(request.getContent());

        String content = "{\"assessment-results\": {\"metadata\": {}}}";
        request.setContent(content);
        assertEquals(content, request.getContent());
    }

    @Test
    void testSetFormat() {
        SarVisualizationRequest request = new SarVisualizationRequest();
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
        SarVisualizationRequest request = new SarVisualizationRequest();
        assertNull(request.getFileName());

        String fileName = "assessment-results.json";
        request.setFileName(fileName);
        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testSetAllFieldsToNull() {
        SarVisualizationRequest request = new SarVisualizationRequest("{}", OscalFormat.JSON, "file.json");

        request.setContent(null);
        request.setFormat(null);
        request.setFileName(null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testWithJsonContent() {
        String jsonContent = "{\"assessment-results\": {\"uuid\": \"12345\", \"metadata\": {\"title\": \"Test SAR\"}}}";

        SarVisualizationRequest request = new SarVisualizationRequest(jsonContent, OscalFormat.JSON, "test-sar.json");

        assertEquals(jsonContent, request.getContent());
        assertEquals(OscalFormat.JSON, request.getFormat());
        assertEquals("test-sar.json", request.getFileName());
    }

    @Test
    void testWithXmlContent() {
        String xmlContent = "<?xml version=\"1.0\"?><assessment-results><uuid>12345</uuid></assessment-results>";

        SarVisualizationRequest request = new SarVisualizationRequest(xmlContent, OscalFormat.XML, "test-sar.xml");

        assertEquals(xmlContent, request.getContent());
        assertEquals(OscalFormat.XML, request.getFormat());
        assertEquals("test-sar.xml", request.getFileName());
    }

    @Test
    void testWithYamlContent() {
        String yamlContent = "assessment-results:\n  uuid: 12345\n  metadata:\n    title: Test SAR";

        SarVisualizationRequest request = new SarVisualizationRequest(yamlContent, OscalFormat.YAML, "test-sar.yaml");

        assertEquals(yamlContent, request.getContent());
        assertEquals(OscalFormat.YAML, request.getFormat());
        assertEquals("test-sar.yaml", request.getFileName());
    }

    @Test
    void testWithLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("{\"assessment-results\": {\"data\": \"test\"}}");
        }

        SarVisualizationRequest request = new SarVisualizationRequest();
        request.setContent(longContent.toString());

        assertTrue(request.getContent().length() > 1000);
    }

    @Test
    void testWithEmptyContent() {
        SarVisualizationRequest request = new SarVisualizationRequest("", OscalFormat.JSON);

        assertEquals("", request.getContent());
        assertEquals(OscalFormat.JSON, request.getFormat());
    }

    @Test
    void testWithEmptyFileName() {
        SarVisualizationRequest request = new SarVisualizationRequest("{}", OscalFormat.JSON, "");

        assertEquals("", request.getFileName());
    }

    @Test
    void testWithSpecialCharactersInFileName() {
        String fileName = "my-test_file (v1.2).json";
        SarVisualizationRequest request = new SarVisualizationRequest("{}", OscalFormat.JSON, fileName);

        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testWithPathInFileName() {
        String fileName = "/path/to/files/sar.json";
        SarVisualizationRequest request = new SarVisualizationRequest();
        request.setFileName(fileName);

        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testModifyAllFields() {
        SarVisualizationRequest request = new SarVisualizationRequest("{}", OscalFormat.JSON, "old.json");

        String newContent = "<assessment-results></assessment-results>";
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
        SarVisualizationRequest request = new SarVisualizationRequest(null, null, null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testTwoArgsConstructorWithNulls() {
        SarVisualizationRequest request = new SarVisualizationRequest(null, null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }
}
