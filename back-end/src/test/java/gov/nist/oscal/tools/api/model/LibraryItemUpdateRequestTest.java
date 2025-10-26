package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LibraryItemUpdateRequestTest {

    @Test
    void testNoArgsConstructor() {
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest();

        assertNotNull(request);
        assertNull(request.getTitle());
        assertNull(request.getDescription());
        assertNull(request.getTags());
    }

    @Test
    void testAllArgsConstructor() {
        Set<String> tags = Set.of("security", "compliance", "production");
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest(
                "Updated System Security Plan",
                "Updated production SSP with new controls",
                tags
        );

        assertEquals("Updated System Security Plan", request.getTitle());
        assertEquals("Updated production SSP with new controls", request.getDescription());
        assertEquals(3, request.getTags().size());
        assertTrue(request.getTags().contains("security"));
    }

    @Test
    void testAllArgsConstructorWithNullValues() {
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest(
                null,
                null,
                null
        );

        assertNull(request.getTitle());
        assertNull(request.getDescription());
        assertNull(request.getTags());
    }

    @Test
    void testSetTitle() {
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest();
        request.setTitle("New Security Plan");
        assertEquals("New Security Plan", request.getTitle());
    }

    @Test
    void testSetDescription() {
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest();
        request.setDescription("Updated description with new compliance requirements");
        assertEquals("Updated description with new compliance requirements", request.getDescription());
    }

    @Test
    void testSetTags() {
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest();
        Set<String> tags = Set.of("tag1", "tag2", "tag3");
        request.setTags(tags);
        assertEquals(tags, request.getTags());
        assertEquals(3, request.getTags().size());
    }

    @Test
    void testWithEmptyTags() {
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest();
        request.setTags(new HashSet<>());
        assertNotNull(request.getTags());
        assertEquals(0, request.getTags().size());
    }

    @Test
    void testWithMultipleTags() {
        Set<String> tags = Set.of("nist", "800-53", "security", "compliance", "ato");
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest(
                "Multi-tag SSP",
                "Security plan with multiple tags",
                tags
        );

        assertEquals(5, request.getTags().size());
        assertTrue(request.getTags().contains("nist"));
        assertTrue(request.getTags().contains("800-53"));
        assertTrue(request.getTags().contains("ato"));
    }

    @Test
    void testSetAllFieldsToNull() {
        Set<String> tags = Set.of("tag1", "tag2");
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest(
                "Title",
                "Description",
                tags
        );

        request.setTitle(null);
        request.setDescription(null);
        request.setTags(null);

        assertNull(request.getTitle());
        assertNull(request.getDescription());
        assertNull(request.getTags());
    }

    @Test
    void testModifyAllFields() {
        Set<String> tags1 = Set.of("old-tag1", "old-tag2");
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest(
                "Old Title",
                "Old Description",
                tags1
        );

        Set<String> tags2 = Set.of("new-tag1", "new-tag2", "new-tag3");
        request.setTitle("New Title");
        request.setDescription("New Description");
        request.setTags(tags2);

        assertEquals("New Title", request.getTitle());
        assertEquals("New Description", request.getDescription());
        assertEquals(3, request.getTags().size());
        assertTrue(request.getTags().contains("new-tag1"));
    }

    @Test
    void testWithLongDescription() {
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest();
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longDesc.append("This library item has been updated with significant changes. ");
        }
        request.setDescription(longDesc.toString());
        assertTrue(request.getDescription().length() > 1000);
    }

    @Test
    void testWithSpecialCharactersInTitle() {
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest();
        String titleWithSpecialChars = "SSP v2.0 (Updated) [2025-03] - Final";
        request.setTitle(titleWithSpecialChars);
        assertEquals(titleWithSpecialChars, request.getTitle());
        assertTrue(request.getTitle().contains("(Updated)"));
    }

    @Test
    void testWithEmptyStrings() {
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest(
                "",
                "",
                new HashSet<>()
        );

        assertEquals("", request.getTitle());
        assertEquals("", request.getDescription());
        assertEquals(0, request.getTags().size());
    }

    @Test
    void testCompleteUpdateScenario() {
        Set<String> tags = Set.of("updated", "reviewed", "approved");
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest(
                "Production SSP - Updated for Q1 2025",
                "Updated security plan incorporating new NIST 800-53 Rev 5 controls and continuous monitoring requirements",
                tags
        );

        assertNotNull(request);
        assertTrue(request.getTitle().contains("Updated"));
        assertTrue(request.getDescription().contains("NIST 800-53"));
        assertTrue(request.getTags().contains("approved"));
    }

    @Test
    void testPartialUpdate() {
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest();
        // Only update title, leave description and tags null
        request.setTitle("Partially Updated Title");

        assertEquals("Partially Updated Title", request.getTitle());
        assertNull(request.getDescription());
        assertNull(request.getTags());
    }

    @Test
    void testTagsOnly() {
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest();
        Set<String> tags = Set.of("v2.0", "production", "active");
        request.setTags(tags);

        assertNull(request.getTitle());
        assertNull(request.getDescription());
        assertEquals(3, request.getTags().size());
    }
}
