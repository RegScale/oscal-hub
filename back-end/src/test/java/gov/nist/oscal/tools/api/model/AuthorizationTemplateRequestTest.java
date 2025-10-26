package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationTemplateRequestTest {

    @Test
    void testNoArgsConstructor() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();

        assertNotNull(request);
        assertNull(request.getName());
        assertNull(request.getContent());
    }

    @Test
    void testAllArgsConstructor() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest(
                "ATO Template",
                "This is to certify that {{systemName}} has been authorized..."
        );

        assertEquals("ATO Template", request.getName());
        assertTrue(request.getContent().contains("{{systemName}}"));
    }

    @Test
    void testSetName() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        request.setName("Production ATO Template");
        assertEquals("Production ATO Template", request.getName());
    }

    @Test
    void testSetContent() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        String content = "Authorization template content with {{variables}}";
        request.setContent(content);
        assertEquals(content, request.getContent());
    }

    @Test
    void testSetAllFields() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        request.setName("Standard ATO");
        request.setContent("Standard authorization template");

        assertEquals("Standard ATO", request.getName());
        assertEquals("Standard authorization template", request.getContent());
    }

    @Test
    void testSetFieldsToNull() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest(
                "Template Name",
                "Template Content"
        );

        request.setName(null);
        request.setContent(null);

        assertNull(request.getName());
        assertNull(request.getContent());
    }

    @Test
    void testModifyAllFields() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();

        request.setName("Old Template");
        request.setContent("Old content");

        request.setName("New Template");
        request.setContent("New content");

        assertEquals("New Template", request.getName());
        assertEquals("New content", request.getContent());
    }

    @Test
    void testWithEmptyStrings() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest("", "");

        assertEquals("", request.getName());
        assertEquals("", request.getContent());
    }

    @Test
    void testWithSpecialCharactersInName() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        String nameWithSpecialChars = "ATO Template v2.0 (Updated) [2025-Q1]";
        request.setName(nameWithSpecialChars);
        assertEquals(nameWithSpecialChars, request.getName());
        assertTrue(request.getName().contains("(Updated)"));
    }

    @Test
    void testWithMultilineContent() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        String multilineContent = "Line 1\nLine 2\nLine 3\n{{variable}}";
        request.setContent(multilineContent);
        assertTrue(request.getContent().contains("\n"));
        assertTrue(request.getContent().contains("{{variable}}"));
    }

    @Test
    void testWithLongContent() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longContent.append("This is a long authorization template paragraph. ");
        }
        request.setContent(longContent.toString());
        assertTrue(request.getContent().length() > 1000);
    }

    @Test
    void testWithHTMLContent() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        String htmlContent = "<html><body><h1>Authorization Letter</h1><p>{{content}}</p></body></html>";
        request.setContent(htmlContent);
        assertTrue(request.getContent().contains("<html>"));
        assertTrue(request.getContent().contains("{{content}}"));
    }

    @Test
    void testWithMarkdownContent() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        String markdownContent = "# Authorization\n\n## System: {{systemName}}\n\n- Date: {{date}}\n- Owner: {{owner}}";
        request.setContent(markdownContent);
        assertTrue(request.getContent().contains("# Authorization"));
        assertTrue(request.getContent().contains("{{systemName}}"));
    }

    @Test
    void testWithMultipleVariables() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest(
                "Multi-Variable Template",
                "System: {{systemName}}, Owner: {{owner}}, Date: {{date}}, Level: {{level}}"
        );

        assertTrue(request.getContent().contains("{{systemName}}"));
        assertTrue(request.getContent().contains("{{owner}}"));
        assertTrue(request.getContent().contains("{{date}}"));
        assertTrue(request.getContent().contains("{{level}}"));
    }

    @Test
    void testCompleteTemplateScenario() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest(
                "FY2025 ATO Template - High Impact",
                "AUTHORIZATION TO OPERATE\n\n" +
                "System Name: {{systemName}}\n" +
                "System Owner: {{systemOwner}}\n" +
                "Authorization Date: {{dateAuthorized}}\n" +
                "Expiration Date: {{dateExpired}}\n\n" +
                "This authorizes {{systemName}} to operate in the {{environment}} environment.\n\n" +
                "Authorizing Official: {{authorizingOfficial}}\n" +
                "Security Manager: {{securityManager}}"
        );

        assertNotNull(request.getName());
        assertNotNull(request.getContent());
        assertTrue(request.getName().contains("FY2025"));
        assertTrue(request.getName().contains("High Impact"));
        assertTrue(request.getContent().contains("AUTHORIZATION TO OPERATE"));
        assertTrue(request.getContent().contains("{{systemName}}"));
        assertTrue(request.getContent().contains("{{systemOwner}}"));
    }

    @Test
    void testWithWhitespace() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest(
                "  Template  ",
                "  Content  "
        );

        assertEquals("  Template  ", request.getName());
        assertEquals("  Content  ", request.getContent());
    }

    @Test
    void testConstructorAndSettersCombined() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest(
                "Initial Name",
                "Initial Content"
        );

        assertEquals("Initial Name", request.getName());
        assertEquals("Initial Content", request.getContent());

        request.setName("Updated Name");
        request.setContent("Updated Content");

        assertEquals("Updated Name", request.getName());
        assertEquals("Updated Content", request.getContent());
    }

    @Test
    void testWithJSONContent() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        String jsonContent = "{\"system\": \"{{systemName}}\", \"date\": \"{{date}}\"}";
        request.setContent(jsonContent);
        assertTrue(request.getContent().contains("{{systemName}}"));
        assertTrue(request.getContent().startsWith("{"));
    }

    @Test
    void testWithXMLContent() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        String xmlContent = "<?xml version=\"1.0\"?><authorization><system>{{systemName}}</system></authorization>";
        request.setContent(xmlContent);
        assertTrue(request.getContent().contains("<?xml"));
        assertTrue(request.getContent().contains("{{systemName}}"));
    }

    @Test
    void testWithUnicodeCharacters() {
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest(
                "授权模板",
                "系统名称: {{systemName}}"
        );
        assertEquals("授权模板", request.getName());
        assertTrue(request.getContent().contains("{{systemName}}"));
    }
}
