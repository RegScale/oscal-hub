package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileVisualizationRequestTest {

    @Test
    void testNoArgsConstructor() {
        ProfileVisualizationRequest request = new ProfileVisualizationRequest();

        assertNotNull(request);
        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testTwoArgsConstructor() {
        String content = "{\"profile\": {}}";
        OscalFormat format = OscalFormat.JSON;

        ProfileVisualizationRequest request = new ProfileVisualizationRequest(content, format);

        assertNotNull(request);
        assertEquals(content, request.getContent());
        assertEquals(format, request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testThreeArgsConstructor() {
        String content = "<profile></profile>";
        OscalFormat format = OscalFormat.XML;
        String fileName = "profile.xml";

        ProfileVisualizationRequest request = new ProfileVisualizationRequest(content, format, fileName);

        assertNotNull(request);
        assertEquals(content, request.getContent());
        assertEquals(format, request.getFormat());
        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testSetContent() {
        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
        assertNull(request.getContent());

        String content = "{\"profile\": {\"metadata\": {}}}";
        request.setContent(content);
        assertEquals(content, request.getContent());
    }

    @Test
    void testSetFormat() {
        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
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
        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
        assertNull(request.getFileName());

        String fileName = "profile.json";
        request.setFileName(fileName);
        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testSetAllFieldsToNull() {
        ProfileVisualizationRequest request = new ProfileVisualizationRequest("{}", OscalFormat.JSON, "file.json");

        request.setContent(null);
        request.setFormat(null);
        request.setFileName(null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testWithJsonContent() {
        String jsonContent = "{\"profile\": {\"uuid\": \"12345\", \"metadata\": {\"title\": \"Test Profile\"}}}";

        ProfileVisualizationRequest request = new ProfileVisualizationRequest(jsonContent, OscalFormat.JSON, "test-profile.json");

        assertEquals(jsonContent, request.getContent());
        assertEquals(OscalFormat.JSON, request.getFormat());
        assertEquals("test-profile.json", request.getFileName());
    }

    @Test
    void testWithXmlContent() {
        String xmlContent = "<?xml version=\"1.0\"?><profile><uuid>12345</uuid></profile>";

        ProfileVisualizationRequest request = new ProfileVisualizationRequest(xmlContent, OscalFormat.XML, "test-profile.xml");

        assertEquals(xmlContent, request.getContent());
        assertEquals(OscalFormat.XML, request.getFormat());
        assertEquals("test-profile.xml", request.getFileName());
    }

    @Test
    void testWithYamlContent() {
        String yamlContent = "profile:\n  uuid: 12345\n  metadata:\n    title: Test Profile";

        ProfileVisualizationRequest request = new ProfileVisualizationRequest(yamlContent, OscalFormat.YAML, "test-profile.yaml");

        assertEquals(yamlContent, request.getContent());
        assertEquals(OscalFormat.YAML, request.getFormat());
        assertEquals("test-profile.yaml", request.getFileName());
    }

    @Test
    void testWithLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("{\"profile\": {\"data\": \"test\"}}");
        }

        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
        request.setContent(longContent.toString());

        assertTrue(request.getContent().length() > 1000);
    }

    @Test
    void testWithEmptyContent() {
        ProfileVisualizationRequest request = new ProfileVisualizationRequest("", OscalFormat.JSON);

        assertEquals("", request.getContent());
        assertEquals(OscalFormat.JSON, request.getFormat());
    }

    @Test
    void testWithEmptyFileName() {
        ProfileVisualizationRequest request = new ProfileVisualizationRequest("{}", OscalFormat.JSON, "");

        assertEquals("", request.getFileName());
    }

    @Test
    void testWithSpecialCharactersInFileName() {
        String fileName = "my-test_file (v1.2).json";
        ProfileVisualizationRequest request = new ProfileVisualizationRequest("{}", OscalFormat.JSON, fileName);

        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testWithPathInFileName() {
        String fileName = "/path/to/files/profile.json";
        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
        request.setFileName(fileName);

        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testModifyAllFields() {
        ProfileVisualizationRequest request = new ProfileVisualizationRequest("{}", OscalFormat.JSON, "old.json");

        String newContent = "<profile></profile>";
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
        ProfileVisualizationRequest request = new ProfileVisualizationRequest(null, null, null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testTwoArgsConstructorWithNulls() {
        ProfileVisualizationRequest request = new ProfileVisualizationRequest(null, null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }
}
