package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LibraryVersionRequestTest {

    @Test
    void testNoArgsConstructor() {
        LibraryVersionRequest request = new LibraryVersionRequest();

        assertNotNull(request);
        assertNull(request.getFileName());
        assertNull(request.getFormat());
        assertNull(request.getFileContent());
        assertNull(request.getChangeDescription());
    }

    @Test
    void testAllArgsConstructor() {
        LibraryVersionRequest request = new LibraryVersionRequest(
                "system-security-plan-v2.json",
                "JSON",
                "{\"system-security-plan\": {\"version\": \"2.0\"}}",
                "Updated security controls to align with NIST 800-53 Rev 5"
        );

        assertEquals("system-security-plan-v2.json", request.getFileName());
        assertEquals("JSON", request.getFormat());
        assertTrue(request.getFileContent().contains("version"));
        assertEquals("Updated security controls to align with NIST 800-53 Rev 5", request.getChangeDescription());
    }

    @Test
    void testAllArgsConstructorWithNullChangeDescription() {
        LibraryVersionRequest request = new LibraryVersionRequest(
                "catalog.xml",
                "XML",
                "<catalog></catalog>",
                null
        );

        assertEquals("catalog.xml", request.getFileName());
        assertEquals("XML", request.getFormat());
        assertEquals("<catalog></catalog>", request.getFileContent());
        assertNull(request.getChangeDescription());
    }

    @Test
    void testSetFileName() {
        LibraryVersionRequest request = new LibraryVersionRequest();
        request.setFileName("assessment-results-v3.yaml");
        assertEquals("assessment-results-v3.yaml", request.getFileName());
    }

    @Test
    void testSetFormat() {
        LibraryVersionRequest request = new LibraryVersionRequest();
        request.setFormat("YAML");
        assertEquals("YAML", request.getFormat());
    }

    @Test
    void testSetFileContent() {
        LibraryVersionRequest request = new LibraryVersionRequest();
        String content = "assessment-results:\n  metadata:\n    version: 3.0";
        request.setFileContent(content);
        assertEquals(content, request.getFileContent());
    }

    @Test
    void testSetChangeDescription() {
        LibraryVersionRequest request = new LibraryVersionRequest();
        String description = "Major update with enhanced risk assessment framework";
        request.setChangeDescription(description);
        assertEquals(description, request.getChangeDescription());
    }

    @Test
    void testWithAllFormats() {
        String[] formats = {"JSON", "XML", "YAML"};

        for (String format : formats) {
            LibraryVersionRequest request = new LibraryVersionRequest();
            request.setFormat(format);
            assertEquals(format, request.getFormat());
        }
    }

    @Test
    void testWithEmptyStrings() {
        LibraryVersionRequest request = new LibraryVersionRequest();
        request.setFileName("");
        request.setFormat("");
        request.setFileContent("");
        request.setChangeDescription("");

        assertEquals("", request.getFileName());
        assertEquals("", request.getFormat());
        assertEquals("", request.getFileContent());
        assertEquals("", request.getChangeDescription());
    }

    @Test
    void testWithLongChangeDescription() {
        LibraryVersionRequest request = new LibraryVersionRequest();
        StringBuilder longDescription = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longDescription.append("This version includes significant updates to security controls. ");
        }
        request.setChangeDescription(longDescription.toString());
        assertTrue(request.getChangeDescription().length() > 1000);
    }

    @Test
    void testWithLongFileContent() {
        LibraryVersionRequest request = new LibraryVersionRequest();
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            longContent.append("{\"control\": \"AC-").append(i).append("\"}, ");
        }
        request.setFileContent(longContent.toString());
        assertTrue(request.getFileContent().length() > 5000);
    }

    @Test
    void testWithSpecialCharactersInFileName() {
        LibraryVersionRequest request = new LibraryVersionRequest();
        String fileName = "ssp-v2.0 (draft) [updated-2025].json";
        request.setFileName(fileName);
        assertEquals(fileName, request.getFileName());
    }

    @Test
    void testSetAllFieldsToNull() {
        LibraryVersionRequest request = new LibraryVersionRequest();
        request.setFileName("file");
        request.setFormat("format");
        request.setFileContent("content");
        request.setChangeDescription("description");

        request.setFileName(null);
        request.setFormat(null);
        request.setFileContent(null);
        request.setChangeDescription(null);

        assertNull(request.getFileName());
        assertNull(request.getFormat());
        assertNull(request.getFileContent());
        assertNull(request.getChangeDescription());
    }

    @Test
    void testModifyAllFields() {
        LibraryVersionRequest request = new LibraryVersionRequest();

        request.setFileName("old-file.json");
        request.setFormat("JSON");
        request.setFileContent("old content");
        request.setChangeDescription("old description");

        request.setFileName("new-file.xml");
        request.setFormat("XML");
        request.setFileContent("new content");
        request.setChangeDescription("new description");

        assertEquals("new-file.xml", request.getFileName());
        assertEquals("XML", request.getFormat());
        assertEquals("new content", request.getFileContent());
        assertEquals("new description", request.getChangeDescription());
    }

    @Test
    void testCompleteVersionUploadScenario() {
        LibraryVersionRequest request = new LibraryVersionRequest(
                "production-ssp-v2.1.json",
                "JSON",
                "{\"system-security-plan\": {\"uuid\": \"abc-123\", \"metadata\": {\"version\": \"2.1\"}}}",
                "Version 2.1: Added continuous monitoring controls and updated incident response procedures"
        );

        assertNotNull(request);
        assertTrue(request.getFileName().contains("v2.1"));
        assertEquals("JSON", request.getFormat());
        assertTrue(request.getFileContent().contains("uuid"));
        assertTrue(request.getChangeDescription().contains("continuous monitoring"));
    }

    @Test
    void testWithNoChangeDescription() {
        LibraryVersionRequest request = new LibraryVersionRequest();
        request.setFileName("minor-update.yaml");
        request.setFormat("YAML");
        request.setFileContent("content: updated");
        // No change description - optional field

        assertEquals("minor-update.yaml", request.getFileName());
        assertNull(request.getChangeDescription());
    }

    @Test
    void testWithMultilineContent() {
        LibraryVersionRequest request = new LibraryVersionRequest();
        String multilineContent = "catalog:\n  metadata:\n    title: NIST SP 800-53\n  controls:\n    - id: AC-1";
        request.setFileContent(multilineContent);
        assertTrue(request.getFileContent().contains("\n"));
        assertTrue(request.getFileContent().contains("controls"));
    }

    @Test
    void testMinimalVersionRequest() {
        LibraryVersionRequest request = new LibraryVersionRequest(
                "update.json",
                "JSON",
                "{}",
                null
        );

        assertEquals("update.json", request.getFileName());
        assertEquals("JSON", request.getFormat());
        assertEquals("{}", request.getFileContent());
        assertNull(request.getChangeDescription());
    }
}
