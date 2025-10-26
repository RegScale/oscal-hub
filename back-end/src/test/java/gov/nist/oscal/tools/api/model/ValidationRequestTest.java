package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRequestTest {

    @Test
    void testNoArgsConstructor() {
        ValidationRequest request = new ValidationRequest();

        assertNotNull(request);
        assertNull(request.getContent());
        assertNull(request.getModelType());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
        assertNull(request.getFileId());
    }

    @Test
    void testThreeArgsConstructor() {
        String content = "{\"catalog\": {}}";
        OscalModelType modelType = OscalModelType.CATALOG;
        OscalFormat format = OscalFormat.JSON;

        ValidationRequest request = new ValidationRequest(content, modelType, format);

        assertNotNull(request);
        assertEquals(content, request.getContent());
        assertEquals(modelType, request.getModelType());
        assertEquals(format, request.getFormat());
        assertNull(request.getFileName());
        assertNull(request.getFileId());
    }

    @Test
    void testFourArgsConstructor() {
        String content = "<profile></profile>";
        OscalModelType modelType = OscalModelType.PROFILE;
        OscalFormat format = OscalFormat.XML;
        String fileName = "profile.xml";

        ValidationRequest request = new ValidationRequest(content, modelType, format, fileName);

        assertNotNull(request);
        assertEquals(content, request.getContent());
        assertEquals(modelType, request.getModelType());
        assertEquals(format, request.getFormat());
        assertEquals(fileName, request.getFileName());
        assertNull(request.getFileId());
    }

    @Test
    void testSetContent() {
        ValidationRequest request = new ValidationRequest();
        assertNull(request.getContent());

        String content = "{\"system-security-plan\": {}}";
        request.setContent(content);
        assertEquals(content, request.getContent());
    }

    @Test
    void testSetModelType() {
        ValidationRequest request = new ValidationRequest();
        assertNull(request.getModelType());

        request.setModelType(OscalModelType.CATALOG);
        assertEquals(OscalModelType.CATALOG, request.getModelType());

        request.setModelType(OscalModelType.SYSTEM_SECURITY_PLAN);
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, request.getModelType());
    }

    @Test
    void testSetFormat() {
        ValidationRequest request = new ValidationRequest();
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
        ValidationRequest request = new ValidationRequest();
        assertNull(request.getFileName());

        String fileName = "validation-test.json";
        request.setFileName(fileName);
        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testSetFileId() {
        ValidationRequest request = new ValidationRequest();
        assertNull(request.getFileId());

        String fileId = "file-12345-abcde";
        request.setFileId(fileId);
        assertEquals(fileId, request.getFileId());
    }

    @Test
    void testWithCatalogModelType() {
        ValidationRequest request = new ValidationRequest(
                "{\"catalog\": {}}",
                OscalModelType.CATALOG,
                OscalFormat.JSON
        );

        assertEquals(OscalModelType.CATALOG, request.getModelType());
    }

    @Test
    void testWithProfileModelType() {
        ValidationRequest request = new ValidationRequest(
                "{\"profile\": {}}",
                OscalModelType.PROFILE,
                OscalFormat.JSON
        );

        assertEquals(OscalModelType.PROFILE, request.getModelType());
    }

    @Test
    void testWithSspModelType() {
        ValidationRequest request = new ValidationRequest(
                "{\"system-security-plan\": {}}",
                OscalModelType.SYSTEM_SECURITY_PLAN,
                OscalFormat.JSON
        );

        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, request.getModelType());
    }

    @Test
    void testWithComponentDefinitionModelType() {
        ValidationRequest request = new ValidationRequest();
        request.setModelType(OscalModelType.COMPONENT_DEFINITION);

        assertEquals(OscalModelType.COMPONENT_DEFINITION, request.getModelType());
    }

    @Test
    void testWithAssessmentPlanModelType() {
        ValidationRequest request = new ValidationRequest();
        request.setModelType(OscalModelType.ASSESSMENT_PLAN);

        assertEquals(OscalModelType.ASSESSMENT_PLAN, request.getModelType());
    }

    @Test
    void testWithAssessmentResultsModelType() {
        ValidationRequest request = new ValidationRequest();
        request.setModelType(OscalModelType.ASSESSMENT_RESULTS);

        assertEquals(OscalModelType.ASSESSMENT_RESULTS, request.getModelType());
    }

    @Test
    void testWithPoamModelType() {
        ValidationRequest request = new ValidationRequest();
        request.setModelType(OscalModelType.PLAN_OF_ACTION_AND_MILESTONES);

        assertEquals(OscalModelType.PLAN_OF_ACTION_AND_MILESTONES, request.getModelType());
    }

    @Test
    void testWithJsonFormat() {
        ValidationRequest request = new ValidationRequest(
                "{}",
                OscalModelType.CATALOG,
                OscalFormat.JSON
        );

        assertEquals(OscalFormat.JSON, request.getFormat());
    }

    @Test
    void testWithXmlFormat() {
        ValidationRequest request = new ValidationRequest(
                "<catalog></catalog>",
                OscalModelType.CATALOG,
                OscalFormat.XML
        );

        assertEquals(OscalFormat.XML, request.getFormat());
    }

    @Test
    void testWithYamlFormat() {
        ValidationRequest request = new ValidationRequest(
                "catalog:\n  uuid: 12345",
                OscalModelType.CATALOG,
                OscalFormat.YAML
        );

        assertEquals(OscalFormat.YAML, request.getFormat());
    }

    @Test
    void testWithFileIdForSavedFile() {
        ValidationRequest request = new ValidationRequest(
                "{\"catalog\": {}}",
                OscalModelType.CATALOG,
                OscalFormat.JSON
        );
        request.setFileId("saved-file-abc123");

        assertEquals("saved-file-abc123", request.getFileId());
        assertNotNull(request.getContent());
    }

    @Test
    void testWithoutFileIdForNewFile() {
        ValidationRequest request = new ValidationRequest(
                "{\"catalog\": {}}",
                OscalModelType.CATALOG,
                OscalFormat.JSON
        );

        assertNull(request.getFileId());
    }

    @Test
    void testWithLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("{\"catalog\": {\"data\": \"test\"}}");
        }

        ValidationRequest request = new ValidationRequest();
        request.setContent(longContent.toString());

        assertTrue(request.getContent().length() > 1000);
    }

    @Test
    void testWithEmptyContent() {
        ValidationRequest request = new ValidationRequest("", OscalModelType.CATALOG, OscalFormat.JSON);

        assertEquals("", request.getContent());
    }

    @Test
    void testWithEmptyFileName() {
        ValidationRequest request = new ValidationRequest(
                "{}", OscalModelType.CATALOG, OscalFormat.JSON, ""
        );

        assertEquals("", request.getFileName());
    }

    @Test
    void testWithSpecialCharactersInFileName() {
        String fileName = "validation-test_file (v1.2).json";
        ValidationRequest request = new ValidationRequest();
        request.setFileName(fileName);

        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testWithPathInFileName() {
        String fileName = "/validation/files/catalog.xml";
        ValidationRequest request = new ValidationRequest();
        request.setFileName(fileName);

        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testSetAllFieldsToNull() {
        ValidationRequest request = new ValidationRequest(
                "{}", OscalModelType.CATALOG, OscalFormat.JSON, "file.json"
        );
        request.setFileId("file-123");

        request.setContent(null);
        request.setModelType(null);
        request.setFormat(null);
        request.setFileName(null);
        request.setFileId(null);

        assertNull(request.getContent());
        assertNull(request.getModelType());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
        assertNull(request.getFileId());
    }

    @Test
    void testModifyAllFields() {
        ValidationRequest request = new ValidationRequest(
                "{}", OscalModelType.CATALOG, OscalFormat.JSON, "old.json"
        );

        String newContent = "<profile></profile>";
        OscalModelType newModelType = OscalModelType.PROFILE;
        OscalFormat newFormat = OscalFormat.XML;
        String newFileName = "new.xml";
        String newFileId = "new-file-456";

        request.setContent(newContent);
        request.setModelType(newModelType);
        request.setFormat(newFormat);
        request.setFileName(newFileName);
        request.setFileId(newFileId);

        assertEquals(newContent, request.getContent());
        assertEquals(newModelType, request.getModelType());
        assertEquals(newFormat, request.getFormat());
        assertEquals(newFileName, request.getFileName());
        assertEquals(newFileId, request.getFileId());
    }

    @Test
    void testConstructorWithNullValues() {
        ValidationRequest request = new ValidationRequest(null, null, null, null);

        assertNull(request.getContent());
        assertNull(request.getModelType());
        assertNull(request.getFormat());
        assertNull(request.getFileName());
    }

    @Test
    void testThreeArgsConstructorWithNulls() {
        ValidationRequest request = new ValidationRequest(null, null, null);

        assertNull(request.getContent());
        assertNull(request.getModelType());
        assertNull(request.getFormat());
    }

    @Test
    void testCompleteValidationScenario() {
        // Simulate a complete validation scenario
        String catalogContent = "{\"catalog\": {\"uuid\": \"xyz-789\", \"metadata\": {\"title\": \"Security Controls\"}}}";

        ValidationRequest request = new ValidationRequest(
                catalogContent,
                OscalModelType.CATALOG,
                OscalFormat.JSON,
                "security-controls-catalog.json"
        );

        assertNotNull(request);
        assertTrue(request.getContent().contains("catalog"));
        assertEquals(OscalModelType.CATALOG, request.getModelType());
        assertEquals(OscalFormat.JSON, request.getFormat());
        assertTrue(request.getFileName().contains("catalog"));
        assertNull(request.getFileId()); // New file, no ID yet
    }

    @Test
    void testRevalidationScenario() {
        // Simulate revalidating an already-saved file
        ValidationRequest request = new ValidationRequest(
                "{\"catalog\": {}}",
                OscalModelType.CATALOG,
                OscalFormat.JSON
        );
        request.setFileId("existing-file-abc123");
        request.setFileName("existing-catalog.json");

        assertNotNull(request.getFileId());
        assertNotNull(request.getFileName());
        assertEquals("existing-file-abc123", request.getFileId());
    }

    @Test
    void testUuidFormatFileId() {
        String uuidFileId = "550e8400-e29b-41d4-a716-446655440000";
        ValidationRequest request = new ValidationRequest();
        request.setFileId(uuidFileId);

        assertEquals(uuidFileId, request.getFileId());
        assertTrue(request.getFileId().contains("-"));
    }
}
