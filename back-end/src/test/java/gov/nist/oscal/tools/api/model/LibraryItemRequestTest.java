package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LibraryItemRequestTest {

    @Test
    void testNoArgsConstructor() {
        LibraryItemRequest request = new LibraryItemRequest();

        assertNotNull(request);
        assertNull(request.getTitle());
        assertNull(request.getDescription());
        assertNull(request.getOscalType());
        assertNull(request.getFileName());
        assertNull(request.getFormat());
        assertNull(request.getFileContent());
        assertNull(request.getTags());
    }

    @Test
    void testAllArgsConstructor() {
        Set<String> tags = Set.of("security", "compliance", "nist");

        LibraryItemRequest request = new LibraryItemRequest(
                "System Security Plan",
                "SSP for production system",
                "system-security-plan",
                "production-ssp.json",
                "JSON",
                "{\"system-security-plan\": {}}",
                tags
        );

        assertEquals("System Security Plan", request.getTitle());
        assertEquals("SSP for production system", request.getDescription());
        assertEquals("system-security-plan", request.getOscalType());
        assertEquals("production-ssp.json", request.getFileName());
        assertEquals("JSON", request.getFormat());
        assertEquals("{\"system-security-plan\": {}}", request.getFileContent());
        assertEquals(3, request.getTags().size());
        assertTrue(request.getTags().contains("security"));
    }

    @Test
    void testAllArgsConstructorWithNullDescription() {
        LibraryItemRequest request = new LibraryItemRequest(
                "Catalog",
                null,
                "catalog",
                "nist-800-53.json",
                "JSON",
                "{\"catalog\": {}}",
                new HashSet<>()
        );

        assertEquals("Catalog", request.getTitle());
        assertNull(request.getDescription());
        assertEquals("catalog", request.getOscalType());
    }

    @Test
    void testAllArgsConstructorWithNullTags() {
        LibraryItemRequest request = new LibraryItemRequest(
                "Profile",
                "Security baseline",
                "profile",
                "baseline.xml",
                "XML",
                "<profile></profile>",
                null
        );

        assertNotNull(request);
        assertNull(request.getTags());
    }

    @Test
    void testSetTitle() {
        LibraryItemRequest request = new LibraryItemRequest();
        request.setTitle("Assessment Plan");
        assertEquals("Assessment Plan", request.getTitle());
    }

    @Test
    void testSetDescription() {
        LibraryItemRequest request = new LibraryItemRequest();
        request.setDescription("Detailed assessment plan for system evaluation");
        assertEquals("Detailed assessment plan for system evaluation", request.getDescription());
    }

    @Test
    void testSetOscalType() {
        LibraryItemRequest request = new LibraryItemRequest();
        request.setOscalType("assessment-plan");
        assertEquals("assessment-plan", request.getOscalType());
    }

    @Test
    void testSetFileName() {
        LibraryItemRequest request = new LibraryItemRequest();
        request.setFileName("assessment.yaml");
        assertEquals("assessment.yaml", request.getFileName());
    }

    @Test
    void testSetFormat() {
        LibraryItemRequest request = new LibraryItemRequest();
        request.setFormat("YAML");
        assertEquals("YAML", request.getFormat());
    }

    @Test
    void testSetFileContent() {
        LibraryItemRequest request = new LibraryItemRequest();
        String content = "assessment-plan:\n  metadata:\n    title: Test";
        request.setFileContent(content);
        assertEquals(content, request.getFileContent());
    }

    @Test
    void testSetTags() {
        LibraryItemRequest request = new LibraryItemRequest();
        Set<String> tags = Set.of("assessment", "evaluation");
        request.setTags(tags);
        assertEquals(2, request.getTags().size());
        assertTrue(request.getTags().contains("assessment"));
    }

    @Test
    void testWithEmptyStrings() {
        LibraryItemRequest request = new LibraryItemRequest();
        request.setTitle("");
        request.setDescription("");
        request.setOscalType("");
        request.setFileName("");
        request.setFormat("");
        request.setFileContent("");

        assertEquals("", request.getTitle());
        assertEquals("", request.getDescription());
        assertEquals("", request.getOscalType());
        assertEquals("", request.getFileName());
        assertEquals("", request.getFormat());
        assertEquals("", request.getFileContent());
    }

    @Test
    void testWithEmptyTagsSet() {
        LibraryItemRequest request = new LibraryItemRequest();
        request.setTags(new HashSet<>());
        assertNotNull(request.getTags());
        assertTrue(request.getTags().isEmpty());
    }

    @Test
    void testWithManyTags() {
        LibraryItemRequest request = new LibraryItemRequest();
        Set<String> tags = Set.of("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tag10");
        request.setTags(tags);
        assertEquals(10, request.getTags().size());
    }

    @Test
    void testWithLongTitle() {
        LibraryItemRequest request = new LibraryItemRequest();
        String longTitle = "Very Long Title ".repeat(20);
        request.setTitle(longTitle);
        assertTrue(request.getTitle().length() > 100);
    }

    @Test
    void testWithLongDescription() {
        LibraryItemRequest request = new LibraryItemRequest();
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longDesc.append("This is a detailed description. ");
        }
        request.setDescription(longDesc.toString());
        assertTrue(request.getDescription().length() > 1000);
    }

    @Test
    void testWithLongFileContent() {
        LibraryItemRequest request = new LibraryItemRequest();
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("{\"field\": \"value\"}, ");
        }
        request.setFileContent(longContent.toString());
        assertTrue(request.getFileContent().length() > 10000);
    }

    @Test
    void testWithSpecialCharactersInFileName() {
        LibraryItemRequest request = new LibraryItemRequest();
        request.setFileName("file-name_v2.0 (draft).json");
        assertEquals("file-name_v2.0 (draft).json", request.getFileName());
    }

    @Test
    void testWithDifferentOscalTypes() {
        String[] oscalTypes = {
                "catalog",
                "profile",
                "component-definition",
                "system-security-plan",
                "assessment-plan",
                "assessment-results",
                "plan-of-action-and-milestones"
        };

        for (String type : oscalTypes) {
            LibraryItemRequest request = new LibraryItemRequest();
            request.setOscalType(type);
            assertEquals(type, request.getOscalType());
        }
    }

    @Test
    void testWithDifferentFormats() {
        String[] formats = {"JSON", "XML", "YAML"};

        for (String format : formats) {
            LibraryItemRequest request = new LibraryItemRequest();
            request.setFormat(format);
            assertEquals(format, request.getFormat());
        }
    }

    @Test
    void testSetAllFieldsToNull() {
        LibraryItemRequest request = new LibraryItemRequest();
        request.setTitle("Title");
        request.setDescription("Description");
        request.setOscalType("Type");
        request.setFileName("File");
        request.setFormat("Format");
        request.setFileContent("Content");
        request.setTags(Set.of("tag"));

        request.setTitle(null);
        request.setDescription(null);
        request.setOscalType(null);
        request.setFileName(null);
        request.setFormat(null);
        request.setFileContent(null);
        request.setTags(null);

        assertNull(request.getTitle());
        assertNull(request.getDescription());
        assertNull(request.getOscalType());
        assertNull(request.getFileName());
        assertNull(request.getFormat());
        assertNull(request.getFileContent());
        assertNull(request.getTags());
    }

    @Test
    void testModifyAllFields() {
        LibraryItemRequest request = new LibraryItemRequest();

        request.setTitle("Old Title");
        request.setDescription("Old Description");
        request.setOscalType("old-type");
        request.setFileName("old-file.json");
        request.setFormat("JSON");
        request.setFileContent("old content");
        request.setTags(Set.of("old"));

        request.setTitle("New Title");
        request.setDescription("New Description");
        request.setOscalType("new-type");
        request.setFileName("new-file.xml");
        request.setFormat("XML");
        request.setFileContent("new content");
        request.setTags(Set.of("new"));

        assertEquals("New Title", request.getTitle());
        assertEquals("New Description", request.getDescription());
        assertEquals("new-type", request.getOscalType());
        assertEquals("new-file.xml", request.getFileName());
        assertEquals("XML", request.getFormat());
        assertEquals("new content", request.getFileContent());
        assertTrue(request.getTags().contains("new"));
        assertFalse(request.getTags().contains("old"));
    }

    @Test
    void testCompleteLibraryItemScenario() {
        Set<String> tags = new HashSet<>();
        tags.add("production");
        tags.add("fedramp");
        tags.add("high");

        LibraryItemRequest request = new LibraryItemRequest(
                "FedRAMP High Baseline SSP",
                "System Security Plan for FedRAMP High baseline compliance",
                "system-security-plan",
                "fedramp-high-ssp.json",
                "JSON",
                "{\"system-security-plan\": {\"uuid\": \"abc123\", \"metadata\": {}}}",
                tags
        );

        assertNotNull(request);
        assertTrue(request.getTitle().contains("FedRAMP"));
        assertTrue(request.getDescription().contains("compliance"));
        assertEquals("system-security-plan", request.getOscalType());
        assertTrue(request.getFileName().endsWith(".json"));
        assertEquals("JSON", request.getFormat());
        assertTrue(request.getFileContent().contains("uuid"));
        assertEquals(3, request.getTags().size());
        assertTrue(request.getTags().contains("fedramp"));
    }
}
