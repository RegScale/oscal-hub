package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReusableElementRequestTest {

    @Test
    void testNoArgsConstructor() {
        ReusableElementRequest request = new ReusableElementRequest();

        assertNotNull(request);
        assertNull(request.getType());
        assertNull(request.getName());
        assertNull(request.getJsonContent());
        assertNull(request.getDescription());
        assertFalse(request.getIsShared()); // default value
    }

    @Test
    void testThreeArgsConstructor() {
        String type = "ROLE";
        String name = "Administrator";
        String jsonContent = "{\"role-id\": \"admin\"}";

        ReusableElementRequest request = new ReusableElementRequest(type, name, jsonContent);

        assertNotNull(request);
        assertEquals(type, request.getType());
        assertEquals(name, request.getName());
        assertEquals(jsonContent, request.getJsonContent());
        assertNull(request.getDescription());
        assertFalse(request.getIsShared());
    }

    @Test
    void testFourArgsConstructor() {
        String type = "PARTY";
        String name = "Organization Alpha";
        String jsonContent = "{\"party-uuid\": \"12345\"}";
        String description = "Primary organization party";

        ReusableElementRequest request = new ReusableElementRequest(type, name, jsonContent, description);

        assertNotNull(request);
        assertEquals(type, request.getType());
        assertEquals(name, request.getName());
        assertEquals(jsonContent, request.getJsonContent());
        assertEquals(description, request.getDescription());
        assertFalse(request.getIsShared());
    }

    @Test
    void testSetType() {
        ReusableElementRequest request = new ReusableElementRequest();
        assertNull(request.getType());

        request.setType("LINK");
        assertEquals("LINK", request.getType());
    }

    @Test
    void testSetName() {
        ReusableElementRequest request = new ReusableElementRequest();
        assertNull(request.getName());

        String name = "Security Contact";
        request.setName(name);
        assertEquals(name, request.getName());
    }

    @Test
    void testSetJsonContent() {
        ReusableElementRequest request = new ReusableElementRequest();
        assertNull(request.getJsonContent());

        String content = "{\"data\": \"value\"}";
        request.setJsonContent(content);
        assertEquals(content, request.getJsonContent());
    }

    @Test
    void testSetDescription() {
        ReusableElementRequest request = new ReusableElementRequest();
        assertNull(request.getDescription());

        String description = "Reusable element for security controls";
        request.setDescription(description);
        assertEquals(description, request.getDescription());
    }

    @Test
    void testSetIsShared() {
        ReusableElementRequest request = new ReusableElementRequest();
        assertFalse(request.getIsShared());

        request.setIsShared(true);
        assertTrue(request.getIsShared());

        request.setIsShared(false);
        assertFalse(request.getIsShared());
    }

    @Test
    void testWithRoleType() {
        ReusableElementRequest request = new ReusableElementRequest(
                "ROLE",
                "System Owner",
                "{\"role-id\": \"owner\", \"title\": \"System Owner\"}"
        );

        assertEquals("ROLE", request.getType());
        assertTrue(request.getJsonContent().contains("role-id"));
    }

    @Test
    void testWithPartyType() {
        ReusableElementRequest request = new ReusableElementRequest(
                "PARTY",
                "Government Agency",
                "{\"party-uuid\": \"abc-123\", \"type\": \"organization\"}"
        );

        assertEquals("PARTY", request.getType());
        assertTrue(request.getJsonContent().contains("party-uuid"));
    }

    @Test
    void testWithLinkType() {
        ReusableElementRequest request = new ReusableElementRequest(
                "LINK",
                "Reference Document",
                "{\"href\": \"https://example.com/doc\", \"rel\": \"reference\"}"
        );

        assertEquals("LINK", request.getType());
        assertTrue(request.getJsonContent().contains("href"));
    }

    @Test
    void testWithBackMatterType() {
        ReusableElementRequest request = new ReusableElementRequest(
                "BACK_MATTER",
                "Supporting Documentation",
                "{\"resources\": []}"
        );

        assertEquals("BACK_MATTER", request.getType());
    }

    @Test
    void testWithResponsiblePartyType() {
        ReusableElementRequest request = new ReusableElementRequest(
                "RESPONSIBLE_PARTY",
                "Primary Contact",
                "{\"role-id\": \"contact\", \"party-uuids\": []}"
        );

        assertEquals("RESPONSIBLE_PARTY", request.getType());
    }

    @Test
    void testWithComplexJsonContent() {
        String complexJson = "{\"role-id\": \"admin\", \"title\": \"Administrator\", \"props\": [{\"name\": \"label\", \"value\": \"Admin\"}]}";
        ReusableElementRequest request = new ReusableElementRequest("ROLE", "Admin Role", complexJson);

        assertEquals(complexJson, request.getJsonContent());
        assertTrue(request.getJsonContent().contains("props"));
    }

    @Test
    void testWithLongDescription() {
        String longDescription = "This is a very long description that contains detailed information about the reusable element, including its purpose, scope, and any special considerations that should be taken into account when using this element in OSCAL documents.";

        ReusableElementRequest request = new ReusableElementRequest(
                "ROLE", "Sample Role", "{}", longDescription
        );

        assertEquals(longDescription, request.getDescription());
        assertTrue(request.getDescription().length() > 100);
    }

    @Test
    void testSharedElement() {
        ReusableElementRequest request = new ReusableElementRequest();
        request.setType("PARTY");
        request.setName("Shared Organization");
        request.setJsonContent("{}");
        request.setIsShared(true);

        assertTrue(request.getIsShared());
    }

    @Test
    void testNonSharedElement() {
        ReusableElementRequest request = new ReusableElementRequest(
                "ROLE", "Private Role", "{}"
        );

        assertFalse(request.getIsShared());
    }

    @Test
    void testSetAllFieldsToNull() {
        ReusableElementRequest request = new ReusableElementRequest(
                "ROLE", "Name", "{}", "Description"
        );

        request.setType(null);
        request.setName(null);
        request.setJsonContent(null);
        request.setDescription(null);
        request.setIsShared(null);

        assertNull(request.getType());
        assertNull(request.getName());
        assertNull(request.getJsonContent());
        assertNull(request.getDescription());
        assertNull(request.getIsShared());
    }

    @Test
    void testWithEmptyStrings() {
        ReusableElementRequest request = new ReusableElementRequest("", "", "");
        request.setDescription("");

        assertEquals("", request.getType());
        assertEquals("", request.getName());
        assertEquals("", request.getJsonContent());
        assertEquals("", request.getDescription());
    }

    @Test
    void testWithSpecialCharactersInName() {
        String nameWithSpecialChars = "Role-Name_With.Special@Characters#123";
        ReusableElementRequest request = new ReusableElementRequest();
        request.setName(nameWithSpecialChars);

        assertEquals(nameWithSpecialChars, request.getName());
    }

    @Test
    void testModifyAllFields() {
        ReusableElementRequest request = new ReusableElementRequest("ROLE", "Old Name", "{}");

        request.setType("PARTY");
        request.setName("New Name");
        request.setJsonContent("{\"new\": \"content\"}");
        request.setDescription("New Description");
        request.setIsShared(true);

        assertEquals("PARTY", request.getType());
        assertEquals("New Name", request.getName());
        assertEquals("{\"new\": \"content\"}", request.getJsonContent());
        assertEquals("New Description", request.getDescription());
        assertTrue(request.getIsShared());
    }

    @Test
    void testConstructorWithNullValues() {
        ReusableElementRequest request = new ReusableElementRequest(null, null, null, null);

        assertNull(request.getType());
        assertNull(request.getName());
        assertNull(request.getJsonContent());
        assertNull(request.getDescription());
        assertFalse(request.getIsShared()); // default value should still be false
    }

    @Test
    void testThreeArgsConstructorWithNulls() {
        ReusableElementRequest request = new ReusableElementRequest(null, null, null);

        assertNull(request.getType());
        assertNull(request.getName());
        assertNull(request.getJsonContent());
    }

    @Test
    void testJsonContentWithEscapedCharacters() {
        String jsonWithEscapes = "{\"title\": \"Quote: \\\"Test\\\"\", \"newline\": \"Line1\\nLine2\"}";
        ReusableElementRequest request = new ReusableElementRequest();
        request.setJsonContent(jsonWithEscapes);

        assertEquals(jsonWithEscapes, request.getJsonContent());
        assertTrue(request.getJsonContent().contains("\\\""));
    }
}
