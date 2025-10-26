package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.AuthorizationTemplateRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthorizationTemplateServiceTest {

    @Mock
    private AuthorizationTemplateRepository templateRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthorizationTemplateService authorizationTemplateService;

    private User mockUser;
    private AuthorizationTemplate mockTemplate;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        mockTemplate = new AuthorizationTemplate(
            "Test Template",
            "Authorization for {{ system_name }} on {{ date }}.",
            mockUser
        );
        mockTemplate.setId(1L);
        mockTemplate.setLastUpdatedBy(mockUser);
    }

    @Test
    void testCreateTemplate_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(templateRepository.save(any(AuthorizationTemplate.class))).thenReturn(mockTemplate);

        AuthorizationTemplate result = authorizationTemplateService.createTemplate(
            "Test Template",
            "Authorization for {{ system_name }} on {{ date }}.",
            "testuser"
        );

        assertNotNull(result);
        assertEquals("Test Template", result.getName());

        verify(userRepository).findByUsername("testuser");
        verify(templateRepository).save(any(AuthorizationTemplate.class));
    }

    @Test
    void testCreateTemplate_userNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authorizationTemplateService.createTemplate("Template", "Content", "nonexistent");
        });

        verify(userRepository).findByUsername("nonexistent");
        verify(templateRepository, never()).save(any());
    }

    @Test
    void testUpdateTemplate_success() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(templateRepository.save(any(AuthorizationTemplate.class))).thenReturn(mockTemplate);

        AuthorizationTemplate result = authorizationTemplateService.updateTemplate(
            1L,
            "Updated Template",
            "Updated content with {{ variable }}.",
            "testuser"
        );

        assertNotNull(result);
        verify(templateRepository).findById(1L);
        verify(templateRepository).save(any(AuthorizationTemplate.class));
    }

    @Test
    void testUpdateTemplate_partialUpdate() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(templateRepository.save(any(AuthorizationTemplate.class))).thenReturn(mockTemplate);

        // Only update name, leave content as null
        AuthorizationTemplate result = authorizationTemplateService.updateTemplate(
            1L,
            "New Name",
            null,
            "testuser"
        );

        assertNotNull(result);
        verify(templateRepository).save(any(AuthorizationTemplate.class));
    }

    @Test
    void testUpdateTemplate_notFound() {
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authorizationTemplateService.updateTemplate(999L, "Name", "Content", "testuser");
        });

        verify(templateRepository).findById(999L);
        verify(templateRepository, never()).save(any());
    }

    @Test
    void testUpdateTemplate_userNotFound() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authorizationTemplateService.updateTemplate(1L, "Name", "Content", "nonexistent");
        });

        verify(templateRepository, never()).save(any());
    }

    @Test
    void testGetTemplate_success() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));

        AuthorizationTemplate result = authorizationTemplateService.getTemplate(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Template", result.getName());

        verify(templateRepository).findById(1L);
    }

    @Test
    void testGetTemplate_notFound() {
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authorizationTemplateService.getTemplate(999L);
        });

        verify(templateRepository).findById(999L);
    }

    @Test
    void testGetAllTemplates_success() {
        when(templateRepository.findAll()).thenReturn(Arrays.asList(mockTemplate));

        List<AuthorizationTemplate> results = authorizationTemplateService.getAllTemplates();

        assertNotNull(results);
        assertEquals(1, results.size());

        verify(templateRepository).findAll();
    }

    @Test
    void testGetRecentlyUpdated_success() {
        AuthorizationTemplate template1 = new AuthorizationTemplate("Template 1", "Content 1", mockUser);
        AuthorizationTemplate template2 = new AuthorizationTemplate("Template 2", "Content 2", mockUser);

        when(templateRepository.findRecentlyUpdated())
            .thenReturn(Arrays.asList(template2, template1));

        List<AuthorizationTemplate> results = authorizationTemplateService.getRecentlyUpdated(2);

        assertNotNull(results);
        assertEquals(2, results.size());

        verify(templateRepository).findRecentlyUpdated();
    }

    @Test
    void testGetRecentlyUpdated_limitExceedsTotal() {
        when(templateRepository.findRecentlyUpdated()).thenReturn(Arrays.asList(mockTemplate));

        List<AuthorizationTemplate> results = authorizationTemplateService.getRecentlyUpdated(10);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGetTemplatesByUser_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(templateRepository.findByCreatedBy(mockUser)).thenReturn(Arrays.asList(mockTemplate));

        List<AuthorizationTemplate> results = authorizationTemplateService.getTemplatesByUser("testuser");

        assertNotNull(results);
        assertEquals(1, results.size());

        verify(userRepository).findByUsername("testuser");
        verify(templateRepository).findByCreatedBy(mockUser);
    }

    @Test
    void testGetTemplatesByUser_userNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authorizationTemplateService.getTemplatesByUser("nonexistent");
        });

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void testSearchTemplates_withSearchTerm() {
        when(templateRepository.findByNameContainingIgnoreCase("authorization"))
            .thenReturn(Arrays.asList(mockTemplate));

        List<AuthorizationTemplate> results = authorizationTemplateService.searchTemplates("authorization");

        assertNotNull(results);
        assertEquals(1, results.size());

        verify(templateRepository).findByNameContainingIgnoreCase("authorization");
    }

    @Test
    void testSearchTemplates_nullSearchTerm() {
        when(templateRepository.findAll()).thenReturn(Arrays.asList(mockTemplate));

        List<AuthorizationTemplate> results = authorizationTemplateService.searchTemplates(null);

        assertNotNull(results);
        verify(templateRepository).findAll();
        verify(templateRepository, never()).findByNameContainingIgnoreCase(any());
    }

    @Test
    void testSearchTemplates_emptySearchTerm() {
        when(templateRepository.findAll()).thenReturn(Arrays.asList(mockTemplate));

        List<AuthorizationTemplate> results = authorizationTemplateService.searchTemplates("");

        assertNotNull(results);
        verify(templateRepository).findAll();
        verify(templateRepository, never()).findByNameContainingIgnoreCase(any());
    }

    @Test
    void testDeleteTemplate_success() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));

        authorizationTemplateService.deleteTemplate(1L, "testuser");

        verify(templateRepository).findById(1L);
        verify(templateRepository).delete(mockTemplate);
    }

    @Test
    void testDeleteTemplate_notFound() {
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authorizationTemplateService.deleteTemplate(999L, "testuser");
        });

        verify(templateRepository).findById(999L);
        verify(templateRepository, never()).delete(any());
    }

    @Test
    void testDeleteTemplate_notCreator() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));

        assertThrows(RuntimeException.class, () -> {
            authorizationTemplateService.deleteTemplate(1L, "otheruser");
        });

        verify(templateRepository).findById(1L);
        verify(templateRepository, never()).delete(any());
    }

    @Test
    void testExtractVariables_basicVariables() {
        String content = "Authorization for {{ system_name }} on {{ date }}.";

        Set<String> variables = authorizationTemplateService.extractVariables(content);

        assertNotNull(variables);
        assertEquals(2, variables.size());
        assertTrue(variables.contains("system_name"));
        assertTrue(variables.contains("date"));
    }

    @Test
    void testExtractVariables_withSpaces() {
        String content = "Authorization for {{  system_name  }} on {{date}}.";

        Set<String> variables = authorizationTemplateService.extractVariables(content);

        assertNotNull(variables);
        assertEquals(2, variables.size());
        assertTrue(variables.contains("system_name"));
        assertTrue(variables.contains("date"));
    }

    @Test
    void testExtractVariables_noVariables() {
        String content = "This is a template without any variables.";

        Set<String> variables = authorizationTemplateService.extractVariables(content);

        assertNotNull(variables);
        assertEquals(0, variables.size());
    }

    @Test
    void testExtractVariables_emptyContent() {
        Set<String> variables = authorizationTemplateService.extractVariables("");

        assertNotNull(variables);
        assertEquals(0, variables.size());
    }

    @Test
    void testExtractVariables_duplicateVariables() {
        String content = "{{ name }} is {{ name }} and {{ name }}.";

        Set<String> variables = authorizationTemplateService.extractVariables(content);

        assertNotNull(variables);
        assertEquals(1, variables.size());
        assertTrue(variables.contains("name"));
    }

    @Test
    void testExtractVariables_complexContent() {
        String content = "Authorization for {{ system.name }} on {{ current_date }} by {{ authorizing_official }}. " +
                        "Expires on {{ expiration.date }}.";

        Set<String> variables = authorizationTemplateService.extractVariables(content);

        assertNotNull(variables);
        assertEquals(4, variables.size());
        assertTrue(variables.contains("system.name"));
        assertTrue(variables.contains("current_date"));
        assertTrue(variables.contains("authorizing_official"));
        assertTrue(variables.contains("expiration.date"));
    }

    @Test
    void testExtractVariables_multiline() {
        String content = "Line 1: {{ var1 }}\n" +
                        "Line 2: {{ var2 }}\n" +
                        "Line 3: {{ var3 }}";

        Set<String> variables = authorizationTemplateService.extractVariables(content);

        assertNotNull(variables);
        assertEquals(3, variables.size());
        assertTrue(variables.contains("var1"));
        assertTrue(variables.contains("var2"));
        assertTrue(variables.contains("var3"));
    }

    @Test
    void testExtractVariablesFromTemplate_success() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));

        Set<String> variables = authorizationTemplateService.extractVariablesFromTemplate(1L);

        assertNotNull(variables);
        assertEquals(2, variables.size());
        assertTrue(variables.contains("system_name"));
        assertTrue(variables.contains("date"));

        verify(templateRepository).findById(1L);
    }

    @Test
    void testExtractVariablesFromTemplate_templateNotFound() {
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authorizationTemplateService.extractVariablesFromTemplate(999L);
        });

        verify(templateRepository).findById(999L);
    }
}
