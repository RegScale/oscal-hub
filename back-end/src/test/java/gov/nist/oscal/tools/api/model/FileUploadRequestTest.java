package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadRequestTest {

    @Test
    void testNoArgsConstructor() {
        FileUploadRequest request = new FileUploadRequest();

        assertNotNull(request);
        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getModelType());
        assertNull(request.getFileName());
    }

    @Test
    void testTwoArgsConstructor() {
        String content = "{\"catalog\": {}}";
        OscalFormat format = OscalFormat.JSON;

        FileUploadRequest request = new FileUploadRequest(content, format);

        assertNotNull(request);
        assertEquals(content, request.getContent());
        assertEquals(format, request.getFormat());
        assertNull(request.getModelType());
        assertNull(request.getFileName());
    }

    @Test
    void testFourArgsConstructor() {
        String content = "<catalog></catalog>";
        OscalFormat format = OscalFormat.XML;
        OscalModelType modelType = OscalModelType.CATALOG;
        String fileName = "catalog.xml";

        FileUploadRequest request = new FileUploadRequest(content, format, modelType, fileName);

        assertNotNull(request);
        assertEquals(content, request.getContent());
        assertEquals(format, request.getFormat());
        assertEquals(modelType, request.getModelType());
        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testSetContent() {
        FileUploadRequest request = new FileUploadRequest();
        assertNull(request.getContent());

        String content = "{\"profile\": {\"metadata\": {}}}";
        request.setContent(content);
        assertEquals(content, request.getContent());
    }

    @Test
    void testSetFormat() {
        FileUploadRequest request = new FileUploadRequest();
        assertNull(request.getFormat());

        request.setFormat(OscalFormat.JSON);
        assertEquals(OscalFormat.JSON, request.getFormat());

        request.setFormat(OscalFormat.XML);
        assertEquals(OscalFormat.XML, request.getFormat());

        request.setFormat(OscalFormat.YAML);
        assertEquals(OscalFormat.YAML, request.getFormat());
    }

    @Test
    void testSetModelType() {
        FileUploadRequest request = new FileUploadRequest();
        assertNull(request.getModelType());

        request.setModelType(OscalModelType.CATALOG);
        assertEquals(OscalModelType.CATALOG, request.getModelType());

        request.setModelType(OscalModelType.PROFILE);
        assertEquals(OscalModelType.PROFILE, request.getModelType());

        request.setModelType(OscalModelType.SYSTEM_SECURITY_PLAN);
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, request.getModelType());
    }

    @Test
    void testSetFileName() {
        FileUploadRequest request = new FileUploadRequest();
        assertNull(request.getFileName());

        String fileName = "upload-file.json";
        request.setFileName(fileName);
        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testWithCatalogModelType() {
        FileUploadRequest request = new FileUploadRequest(
                "{\"catalog\": {}}",
                OscalFormat.JSON,
                OscalModelType.CATALOG,
                "catalog.json"
        );

        assertEquals(OscalModelType.CATALOG, request.getModelType());
    }

    @Test
    void testWithProfileModelType() {
        FileUploadRequest request = new FileUploadRequest(
                "{\"profile\": {}}",
                OscalFormat.JSON,
                OscalModelType.PROFILE,
                "profile.json"
        );

        assertEquals(OscalModelType.PROFILE, request.getModelType());
    }

    @Test
    void testWithSspModelType() {
        FileUploadRequest request = new FileUploadRequest(
                "{\"system-security-plan\": {}}",
                OscalFormat.JSON,
                OscalModelType.SYSTEM_SECURITY_PLAN,
                "ssp.json"
        );

        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, request.getModelType());
    }

    @Test
    void testWithComponentDefinitionModelType() {
        FileUploadRequest request = new FileUploadRequest();
        request.setModelType(OscalModelType.COMPONENT_DEFINITION);

        assertEquals(OscalModelType.COMPONENT_DEFINITION, request.getModelType());
    }

    @Test
    void testWithAssessmentPlanModelType() {
        FileUploadRequest request = new FileUploadRequest();
        request.setModelType(OscalModelType.ASSESSMENT_PLAN);

        assertEquals(OscalModelType.ASSESSMENT_PLAN, request.getModelType());
    }

    @Test
    void testWithAssessmentResultsModelType() {
        FileUploadRequest request = new FileUploadRequest();
        request.setModelType(OscalModelType.ASSESSMENT_RESULTS);

        assertEquals(OscalModelType.ASSESSMENT_RESULTS, request.getModelType());
    }

    @Test
    void testWithPoamModelType() {
        FileUploadRequest request = new FileUploadRequest();
        request.setModelType(OscalModelType.PLAN_OF_ACTION_AND_MILESTONES);

        assertEquals(OscalModelType.PLAN_OF_ACTION_AND_MILESTONES, request.getModelType());
    }

    @Test
    void testWithJsonFormat() {
        FileUploadRequest request = new FileUploadRequest(
                "{\"catalog\": {}}",
                OscalFormat.JSON
        );

        assertEquals(OscalFormat.JSON, request.getFormat());
    }

    @Test
    void testWithXmlFormat() {
        FileUploadRequest request = new FileUploadRequest(
                "<?xml version=\"1.0\"?><catalog></catalog>",
                OscalFormat.XML
        );

        assertEquals(OscalFormat.XML, request.getFormat());
    }

    @Test
    void testWithYamlFormat() {
        FileUploadRequest request = new FileUploadRequest(
                "catalog:\n  uuid: 12345",
                OscalFormat.YAML
        );

        assertEquals(OscalFormat.YAML, request.getFormat());
    }

    @Test
    void testWithLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("{\"data\": \"value\"}");
        }

        FileUploadRequest request = new FileUploadRequest();
        request.setContent(longContent.toString());

        assertTrue(request.getContent().length() > 1000);
    }

    @Test
    void testWithEmptyContent() {
        FileUploadRequest request = new FileUploadRequest("", OscalFormat.JSON);

        assertEquals("", request.getContent());
    }

    @Test
    void testWithEmptyFileName() {
        FileUploadRequest request = new FileUploadRequest(
                "{}", OscalFormat.JSON, OscalModelType.CATALOG, ""
        );

        assertEquals("", request.getFileName());
    }

    @Test
    void testWithSpecialCharactersInFileName() {
        String fileName = "my-file_upload (v1.2).json";
        FileUploadRequest request = new FileUploadRequest();
        request.setFileName(fileName);

        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testWithPathInFileName() {
        String fileName = "/uploads/documents/catalog.xml";
        FileUploadRequest request = new FileUploadRequest();
        request.setFileName(fileName);

        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testSetAllFieldsToNull() {
        FileUploadRequest request = new FileUploadRequest(
                "{}", OscalFormat.JSON, OscalModelType.CATALOG, "file.json"
        );

        request.setContent(null);
        request.setFormat(null);
        request.setModelType(null);
        request.setFileName(null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getModelType());
        assertNull(request.getFileName());
    }

    @Test
    void testModifyAllFields() {
        FileUploadRequest request = new FileUploadRequest(
                "{}", OscalFormat.JSON, OscalModelType.CATALOG, "old.json"
        );

        String newContent = "<profile></profile>";
        OscalFormat newFormat = OscalFormat.XML;
        OscalModelType newModelType = OscalModelType.PROFILE;
        String newFileName = "new.xml";

        request.setContent(newContent);
        request.setFormat(newFormat);
        request.setModelType(newModelType);
        request.setFileName(newFileName);

        assertEquals(newContent, request.getContent());
        assertEquals(newFormat, request.getFormat());
        assertEquals(newModelType, request.getModelType());
        assertEquals(newFileName, request.getFileName());
    }

    @Test
    void testConstructorWithNullValues() {
        FileUploadRequest request = new FileUploadRequest(null, null, null, null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getModelType());
        assertNull(request.getFileName());
    }

    @Test
    void testTwoArgsConstructorWithNulls() {
        FileUploadRequest request = new FileUploadRequest(null, null);

        assertNull(request.getContent());
        assertNull(request.getFormat());
        assertNull(request.getModelType());
        assertNull(request.getFileName());
    }

    @Test
    void testCompleteUploadScenario() {
        // Simulate a complete file upload scenario
        String catalogContent = "{\"catalog\": {\"uuid\": \"abc-123\", \"metadata\": {\"title\": \"NIST SP 800-53\"}}}";

        FileUploadRequest request = new FileUploadRequest(
                catalogContent,
                OscalFormat.JSON,
                OscalModelType.CATALOG,
                "nist-800-53-catalog.json"
        );

        assertNotNull(request);
        assertTrue(request.getContent().contains("catalog"));
        assertEquals(OscalFormat.JSON, request.getFormat());
        assertEquals(OscalModelType.CATALOG, request.getModelType());
        assertTrue(request.getFileName().endsWith(".json"));
    }
}
