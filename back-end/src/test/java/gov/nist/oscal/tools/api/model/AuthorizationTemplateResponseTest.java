package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationTemplateResponseTest {

    @Test
    void testNoArgsConstructor() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();

        assertNotNull(response);
        assertNull(response.getId());
        assertNull(response.getName());
        assertNull(response.getContent());
        assertNull(response.getCreatedBy());
        assertNull(response.getCreatedAt());
        assertNull(response.getLastUpdatedBy());
        assertNull(response.getLastUpdatedAt());
        assertNull(response.getVariables());
    }

    @Test
    void testEntityConstructorWithAllFields() {
        // Create mock users
        User creator = mock(User.class);
        when(creator.getUsername()).thenReturn("john.doe");

        User updater = mock(User.class);
        when(updater.getUsername()).thenReturn("jane.smith");

        // Create mock template
        AuthorizationTemplate template = mock(AuthorizationTemplate.class);
        when(template.getId()).thenReturn(1L);
        when(template.getName()).thenReturn("ATO Template");
        when(template.getContent()).thenReturn("Authorization content with {{variable1}} and {{variable2}}");
        when(template.getCreatedBy()).thenReturn(creator);
        when(template.getCreatedAt()).thenReturn(LocalDateTime.of(2025, 1, 1, 10, 0));
        when(template.getLastUpdatedBy()).thenReturn(updater);
        when(template.getLastUpdatedAt()).thenReturn(LocalDateTime.of(2025, 2, 1, 15, 30));

        Set<String> variables = Set.of("variable1", "variable2");

        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse(template, variables);

        assertEquals(1L, response.getId());
        assertEquals("ATO Template", response.getName());
        assertTrue(response.getContent().contains("variable1"));
        assertEquals("john.doe", response.getCreatedBy());
        assertEquals(LocalDateTime.of(2025, 1, 1, 10, 0), response.getCreatedAt());
        assertEquals("jane.smith", response.getLastUpdatedBy());
        assertEquals(LocalDateTime.of(2025, 2, 1, 15, 30), response.getLastUpdatedAt());
        assertEquals(variables, response.getVariables());
    }

    @Test
    void testEntityConstructorWithNullLastUpdatedBy() {
        // When lastUpdatedBy is null, should fall back to createdBy
        User creator = mock(User.class);
        when(creator.getUsername()).thenReturn("original.user");

        AuthorizationTemplate template = mock(AuthorizationTemplate.class);
        when(template.getId()).thenReturn(2L);
        when(template.getName()).thenReturn("New Template");
        when(template.getContent()).thenReturn("Content");
        when(template.getCreatedBy()).thenReturn(creator);
        when(template.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(template.getLastUpdatedBy()).thenReturn(null); // No updates yet
        when(template.getLastUpdatedAt()).thenReturn(LocalDateTime.now());

        Set<String> variables = new HashSet<>();

        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse(template, variables);

        assertEquals("original.user", response.getCreatedBy());
        assertEquals("original.user", response.getLastUpdatedBy()); // Should be same as createdBy
    }

    @Test
    void testSetId() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();
        assertNull(response.getId());

        response.setId(123L);
        assertEquals(123L, response.getId());
    }

    @Test
    void testSetName() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();
        assertNull(response.getName());

        String name = "Security Authorization Template";
        response.setName(name);
        assertEquals(name, response.getName());
    }

    @Test
    void testSetContent() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();
        assertNull(response.getContent());

        String content = "This is the authorization content with {{systemName}}";
        response.setContent(content);
        assertEquals(content, response.getContent());
    }

    @Test
    void testSetCreatedBy() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();
        assertNull(response.getCreatedBy());

        response.setCreatedBy("admin.user");
        assertEquals("admin.user", response.getCreatedBy());
    }

    @Test
    void testSetCreatedAt() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();
        assertNull(response.getCreatedAt());

        LocalDateTime timestamp = LocalDateTime.of(2025, 3, 15, 9, 30);
        response.setCreatedAt(timestamp);
        assertEquals(timestamp, response.getCreatedAt());
    }

    @Test
    void testSetLastUpdatedBy() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();
        assertNull(response.getLastUpdatedBy());

        response.setLastUpdatedBy("updater.user");
        assertEquals("updater.user", response.getLastUpdatedBy());
    }

    @Test
    void testSetLastUpdatedAt() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();
        assertNull(response.getLastUpdatedAt());

        LocalDateTime timestamp = LocalDateTime.of(2025, 4, 20, 14, 45);
        response.setLastUpdatedAt(timestamp);
        assertEquals(timestamp, response.getLastUpdatedAt());
    }

    @Test
    void testSetVariables() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();
        assertNull(response.getVariables());

        Set<String> variables = Set.of("var1", "var2", "var3");
        response.setVariables(variables);
        assertEquals(variables, response.getVariables());
    }

    @Test
    void testWithEmptyVariablesSet() {
        User creator = mock(User.class);
        when(creator.getUsername()).thenReturn("user");

        AuthorizationTemplate template = mock(AuthorizationTemplate.class);
        when(template.getId()).thenReturn(1L);
        when(template.getName()).thenReturn("Template");
        when(template.getContent()).thenReturn("No variables");
        when(template.getCreatedBy()).thenReturn(creator);
        when(template.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(template.getLastUpdatedBy()).thenReturn(creator);
        when(template.getLastUpdatedAt()).thenReturn(LocalDateTime.now());

        Set<String> emptyVariables = new HashSet<>();

        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse(template, emptyVariables);

        assertNotNull(response.getVariables());
        assertTrue(response.getVariables().isEmpty());
    }

    @Test
    void testWithManyVariables() {
        User creator = mock(User.class);
        when(creator.getUsername()).thenReturn("user");

        AuthorizationTemplate template = mock(AuthorizationTemplate.class);
        when(template.getId()).thenReturn(1L);
        when(template.getName()).thenReturn("Complex Template");
        when(template.getContent()).thenReturn("Content with many variables");
        when(template.getCreatedBy()).thenReturn(creator);
        when(template.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(template.getLastUpdatedBy()).thenReturn(creator);
        when(template.getLastUpdatedAt()).thenReturn(LocalDateTime.now());

        Set<String> manyVariables = Set.of("var1", "var2", "var3", "var4", "var5", "var6", "var7", "var8", "var9", "var10");

        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse(template, manyVariables);

        assertEquals(10, response.getVariables().size());
        assertTrue(response.getVariables().contains("var1"));
        assertTrue(response.getVariables().contains("var10"));
    }

    @Test
    void testSetAllFieldsToNull() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();
        response.setId(1L);
        response.setName("Name");
        response.setContent("Content");
        response.setCreatedBy("user");
        response.setCreatedAt(LocalDateTime.now());
        response.setLastUpdatedBy("updater");
        response.setLastUpdatedAt(LocalDateTime.now());
        response.setVariables(Set.of("var"));

        response.setId(null);
        response.setName(null);
        response.setContent(null);
        response.setCreatedBy(null);
        response.setCreatedAt(null);
        response.setLastUpdatedBy(null);
        response.setLastUpdatedAt(null);
        response.setVariables(null);

        assertNull(response.getId());
        assertNull(response.getName());
        assertNull(response.getContent());
        assertNull(response.getCreatedBy());
        assertNull(response.getCreatedAt());
        assertNull(response.getLastUpdatedBy());
        assertNull(response.getLastUpdatedAt());
        assertNull(response.getVariables());
    }

    @Test
    void testModifyAllFields() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();

        response.setId(1L);
        response.setName("Old Name");
        response.setContent("Old Content");
        response.setCreatedBy("old.user");
        response.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        response.setLastUpdatedBy("old.updater");
        response.setLastUpdatedAt(LocalDateTime.of(2024, 6, 1, 0, 0));
        response.setVariables(Set.of("oldVar"));

        response.setId(2L);
        response.setName("New Name");
        response.setContent("New Content");
        response.setCreatedBy("new.user");
        response.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        response.setLastUpdatedBy("new.updater");
        response.setLastUpdatedAt(LocalDateTime.of(2025, 6, 1, 0, 0));
        response.setVariables(Set.of("newVar"));

        assertEquals(2L, response.getId());
        assertEquals("New Name", response.getName());
        assertEquals("New Content", response.getContent());
        assertEquals("new.user", response.getCreatedBy());
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), response.getCreatedAt());
        assertEquals("new.updater", response.getLastUpdatedBy());
        assertEquals(LocalDateTime.of(2025, 6, 1, 0, 0), response.getLastUpdatedAt());
        assertTrue(response.getVariables().contains("newVar"));
    }

    @Test
    void testWithLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("Long template content {{variable}} ");
        }

        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();
        response.setContent(longContent.toString());

        assertTrue(response.getContent().length() > 10000);
    }

    @Test
    void testWithSpecialCharactersInName() {
        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse();
        String nameWithSpecialChars = "Template (v2.0) [Draft] - New!";
        response.setName(nameWithSpecialChars);

        assertEquals(nameWithSpecialChars, response.getName());
    }

    @Test
    void testCompleteTemplateLifecycle() {
        // Creation
        User creator = mock(User.class);
        when(creator.getUsername()).thenReturn("template.creator");

        AuthorizationTemplate template = mock(AuthorizationTemplate.class);
        when(template.getId()).thenReturn(10L);
        when(template.getName()).thenReturn("System Authorization Template");
        when(template.getContent()).thenReturn("Authorization for {{systemName}} expires on {{expirationDate}}");
        when(template.getCreatedBy()).thenReturn(creator);
        when(template.getCreatedAt()).thenReturn(LocalDateTime.of(2025, 1, 1, 10, 0));
        when(template.getLastUpdatedBy()).thenReturn(null);
        when(template.getLastUpdatedAt()).thenReturn(LocalDateTime.of(2025, 1, 1, 10, 0));

        Set<String> variables = Set.of("systemName", "expirationDate");

        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse(template, variables);

        // Verify creation
        assertEquals(10L, response.getId());
        assertTrue(response.getName().contains("System"));
        assertEquals("template.creator", response.getCreatedBy());
        assertEquals("template.creator", response.getLastUpdatedBy()); // Falls back to creator
        assertEquals(2, response.getVariables().size());
        assertTrue(response.getVariables().contains("systemName"));
        assertTrue(response.getVariables().contains("expirationDate"));
    }

    @Test
    void testWithVariableNamesContainingSpecialCharacters() {
        User creator = mock(User.class);
        when(creator.getUsername()).thenReturn("user");

        AuthorizationTemplate template = mock(AuthorizationTemplate.class);
        when(template.getId()).thenReturn(1L);
        when(template.getName()).thenReturn("Template");
        when(template.getContent()).thenReturn("Content");
        when(template.getCreatedBy()).thenReturn(creator);
        when(template.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(template.getLastUpdatedBy()).thenReturn(creator);
        when(template.getLastUpdatedAt()).thenReturn(LocalDateTime.now());

        Set<String> variables = Set.of("var_name", "var-name", "var.name", "varName123");

        AuthorizationTemplateResponse response = new AuthorizationTemplateResponse(template, variables);

        assertEquals(4, response.getVariables().size());
        assertTrue(response.getVariables().contains("var_name"));
        assertTrue(response.getVariables().contains("var-name"));
    }
}
