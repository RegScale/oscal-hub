package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.ConditionOfApproval;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.ConditionOfApprovalRequest;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import gov.nist.oscal.tools.api.repository.AuthorizationTemplateRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private AuthorizationTemplateRepository templateRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private User mockUser;
    private AuthorizationTemplate mockTemplate;
    private Authorization mockAuthorization;

    @BeforeEach
    void setUp() {
        // Create mock user
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setLogo("https://example.com/logo.png");

        // Create mock template
        mockTemplate = new AuthorizationTemplate();
        mockTemplate.setId(1L);
        mockTemplate.setName("Test Template");
        mockTemplate.setContent("Authorization for {{ system_name }} on {{ date }}. {{ logo }}");
        mockTemplate.setCreatedBy(mockUser);
        mockTemplate.setLastUpdatedBy(mockUser);
        mockTemplate.setCreatedAt(LocalDateTime.now());
        mockTemplate.setLastUpdatedAt(LocalDateTime.now());

        // Create mock authorization
        mockAuthorization = new Authorization();
        mockAuthorization.setId(1L);
        mockAuthorization.setName("Test Authorization");
        mockAuthorization.setSspItemId("ssp-123");
        mockAuthorization.setSarItemId("sar-123");
        mockAuthorization.setTemplate(mockTemplate);
        mockAuthorization.setAuthorizedBy(mockUser);
        mockAuthorization.setDateAuthorized(LocalDate.now());
        mockAuthorization.setDateExpired(LocalDate.now().plusYears(3));
        mockAuthorization.setSystemOwner("John Doe");
        mockAuthorization.setSecurityManager("Jane Smith");
        mockAuthorization.setAuthorizingOfficial("Bob Johnson");
        mockAuthorization.setCompletedContent("Completed content");
    }

    @Test
    void testCreateAuthorization_success_returnsAuthorization() {
        // Arrange
        Map<String, String> variableValues = new HashMap<>();
        variableValues.put("system_name", "Test System");
        variableValues.put("date", "2024-01-01");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));
        when(authorizationRepository.save(any(Authorization.class))).thenAnswer(invocation -> {
            Authorization auth = invocation.getArgument(0);
            auth.setId(1L);
            return auth;
        });

        // Act
        Authorization result = authorizationService.createAuthorization(
                "Test Authorization", "ssp-123", "sar-123", 1L,
                variableValues, "testuser",
                "2024-01-01", "2027-01-01",
                "John Doe", "Jane Smith", "Bob Johnson",
                null, Collections.emptyList()
        );

        // Assert
        assertNotNull(result);
        assertEquals("Test Authorization", result.getName());
        assertEquals("ssp-123", result.getSspItemId());
        assertEquals("sar-123", result.getSarItemId());
        assertNotNull(result.getCompletedContent());
        assertTrue(result.getCompletedContent().contains("Test System"));
        assertTrue(result.getCompletedContent().contains("2024-01-01"));
        assertTrue(result.getCompletedContent().contains("![Logo](https://example.com/logo.png)"));

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(templateRepository, times(1)).findById(1L);
        verify(authorizationRepository, atLeastOnce()).save(any(Authorization.class));
    }

    @Test
    void testCreateAuthorization_withConditions_createsConditions() {
        // Arrange
        Map<String, String> variableValues = new HashMap<>();
        variableValues.put("system_name", "Test System");

        ConditionOfApprovalRequest conditionRequest = new ConditionOfApprovalRequest();
        conditionRequest.setCondition("Complete security assessment");
        conditionRequest.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        conditionRequest.setDueDate("2024-06-01");

        List<ConditionOfApprovalRequest> conditionRequests = Arrays.asList(conditionRequest);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));
        when(authorizationRepository.save(any(Authorization.class))).thenAnswer(invocation -> {
            Authorization auth = invocation.getArgument(0);
            auth.setId(1L);
            return auth;
        });

        // Act
        Authorization result = authorizationService.createAuthorization(
                "Test Authorization", "ssp-123", null, 1L,
                variableValues, "testuser",
                "2024-01-01", "2027-01-01",
                "John Doe", "Jane Smith", "Bob Johnson",
                null, conditionRequests
        );

        // Assert
        assertNotNull(result);
        verify(authorizationRepository, times(2)).save(any(Authorization.class)); // Saved twice: once for auth, once for conditions
    }

    @Test
    void testCreateAuthorization_withEditedContent_usesEditedContent() {
        // Arrange
        Map<String, String> variableValues = new HashMap<>();
        variableValues.put("system_name", "Test System");

        String editedContent = "Custom authorization for {{ system_name }}";

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(templateRepository.findById(1L)).thenReturn(Optional.of(mockTemplate));
        when(authorizationRepository.save(any(Authorization.class))).thenAnswer(invocation -> {
            Authorization auth = invocation.getArgument(0);
            auth.setId(1L);
            return auth;
        });

        // Act
        Authorization result = authorizationService.createAuthorization(
                "Test Authorization", "ssp-123", null, 1L,
                variableValues, "testuser",
                null, null,
                "John Doe", "Jane Smith", "Bob Johnson",
                editedContent, Collections.emptyList()
        );

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCompletedContent());
        assertTrue(result.getCompletedContent().contains("Custom authorization for Test System"));
        assertFalse(result.getCompletedContent().contains("{{ system_name }}"));
    }

    @Test
    void testCreateAuthorization_userNotFound_throwsException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authorizationService.createAuthorization(
                    "Test", "ssp-123", null, 1L,
                    new HashMap<>(), "nonexistent",
                    null, null, "Owner", "Manager", "Official",
                    null, Collections.emptyList()
            );
        });

        assertEquals("User not found: nonexistent", exception.getMessage());
        verify(authorizationRepository, never()).save(any(Authorization.class));
    }

    @Test
    void testCreateAuthorization_templateNotFound_throwsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authorizationService.createAuthorization(
                    "Test", "ssp-123", null, 999L,
                    new HashMap<>(), "testuser",
                    null, null, "Owner", "Manager", "Official",
                    null, Collections.emptyList()
            );
        });

        assertEquals("Template not found: 999", exception.getMessage());
        verify(authorizationRepository, never()).save(any(Authorization.class));
    }

    @Test
    void testUpdateAuthorization_success_updatesAuthorization() {
        // Arrange
        Map<String, String> variableValues = new HashMap<>();
        variableValues.put("system_name", "Updated System");

        when(authorizationRepository.findById(1L)).thenReturn(Optional.of(mockAuthorization));
        when(authorizationRepository.save(any(Authorization.class))).thenReturn(mockAuthorization);

        // Act
        Authorization result = authorizationService.updateAuthorization(
                1L, "Updated Name", variableValues, "testuser",
                "2024-02-01", "2027-02-01",
                "New Owner", "New Manager", "New Official",
                null, Collections.emptyList()
        );

        // Assert
        assertNotNull(result);
        verify(authorizationRepository, times(1)).findById(1L);
        verify(authorizationRepository, times(1)).save(any(Authorization.class));
    }

    @Test
    void testUpdateAuthorization_notFound_throwsException() {
        // Arrange
        when(authorizationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authorizationService.updateAuthorization(
                    999L, "Updated", new HashMap<>(), "testuser",
                    null, null, null, null, null,
                    null, null
            );
        });

        assertEquals("Authorization not found: 999", exception.getMessage());
        verify(authorizationRepository, never()).save(any(Authorization.class));
    }

    @Test
    void testGetAuthorization_success_returnsAuthorization() {
        // Arrange
        when(authorizationRepository.findById(1L)).thenReturn(Optional.of(mockAuthorization));

        // Act
        Authorization result = authorizationService.getAuthorization(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Authorization", result.getName());
        verify(authorizationRepository, times(1)).findById(1L);
    }

    @Test
    void testGetAuthorization_notFound_throwsException() {
        // Arrange
        when(authorizationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authorizationService.getAuthorization(999L);
        });

        assertEquals("Authorization not found: 999", exception.getMessage());
    }

    @Test
    void testGetAllAuthorizations_success_returnsAllAuthorizations() {
        // Arrange
        List<Authorization> authorizations = Arrays.asList(mockAuthorization);
        when(authorizationRepository.findAll()).thenReturn(authorizations);

        // Act
        List<Authorization> result = authorizationService.getAllAuthorizations();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(authorizationRepository, times(1)).findAll();
    }

    @Test
    void testGetRecentlyAuthorized_success_returnsLimitedResults() {
        // Arrange
        List<Authorization> authorizations = Arrays.asList(mockAuthorization, mockAuthorization, mockAuthorization);
        when(authorizationRepository.findRecentlyAuthorized()).thenReturn(authorizations);

        // Act
        List<Authorization> result = authorizationService.getRecentlyAuthorized(2);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(authorizationRepository, times(1)).findRecentlyAuthorized();
    }

    @Test
    void testGetAuthorizationsBySsp_success_returnsAuthorizations() {
        // Arrange
        List<Authorization> authorizations = Arrays.asList(mockAuthorization);
        when(authorizationRepository.findBySspItemId("ssp-123")).thenReturn(authorizations);

        // Act
        List<Authorization> result = authorizationService.getAuthorizationsBySsp("ssp-123");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(authorizationRepository, times(1)).findBySspItemId("ssp-123");
    }

    @Test
    void testGetAuthorizationsByUser_success_returnsAuthorizations() {
        // Arrange
        List<Authorization> authorizations = Arrays.asList(mockAuthorization);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(authorizationRepository.findByAuthorizedBy(mockUser)).thenReturn(authorizations);

        // Act
        List<Authorization> result = authorizationService.getAuthorizationsByUser("testuser");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(authorizationRepository, times(1)).findByAuthorizedBy(mockUser);
    }

    @Test
    void testGetAuthorizationsByUser_userNotFound_throwsException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authorizationService.getAuthorizationsByUser("nonexistent");
        });

        assertEquals("User not found: nonexistent", exception.getMessage());
    }

    @Test
    void testSearchAuthorizations_withSearchTerm_returnsMatchingAuthorizations() {
        // Arrange
        List<Authorization> authorizations = Arrays.asList(mockAuthorization);
        when(authorizationRepository.searchByNameOrSspItemId("test")).thenReturn(authorizations);

        // Act
        List<Authorization> result = authorizationService.searchAuthorizations("test");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(authorizationRepository, times(1)).searchByNameOrSspItemId("test");
    }

    @Test
    void testSearchAuthorizations_nullSearchTerm_returnsAllAuthorizations() {
        // Arrange
        List<Authorization> authorizations = Arrays.asList(mockAuthorization);
        when(authorizationRepository.findAll()).thenReturn(authorizations);

        // Act
        List<Authorization> result = authorizationService.searchAuthorizations(null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(authorizationRepository, times(1)).findAll();
        verify(authorizationRepository, never()).searchByNameOrSspItemId(anyString());
    }

    @Test
    void testSearchAuthorizations_emptySearchTerm_returnsAllAuthorizations() {
        // Arrange
        List<Authorization> authorizations = Arrays.asList(mockAuthorization);
        when(authorizationRepository.findAll()).thenReturn(authorizations);

        // Act
        List<Authorization> result = authorizationService.searchAuthorizations("");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(authorizationRepository, times(1)).findAll();
        verify(authorizationRepository, never()).searchByNameOrSspItemId(anyString());
    }

    @Test
    void testDeleteAuthorization_success_deletesAuthorization() {
        // Arrange
        when(authorizationRepository.findById(1L)).thenReturn(Optional.of(mockAuthorization));

        // Act
        authorizationService.deleteAuthorization(1L, "testuser");

        // Assert
        verify(authorizationRepository, times(1)).findById(1L);
        verify(authorizationRepository, times(1)).delete(mockAuthorization);
    }

    @Test
    void testDeleteAuthorization_notCreator_throwsException() {
        // Arrange
        when(authorizationRepository.findById(1L)).thenReturn(Optional.of(mockAuthorization));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authorizationService.deleteAuthorization(1L, "otheruser");
        });

        assertEquals("Only the creator can delete this authorization", exception.getMessage());
        verify(authorizationRepository, never()).delete(any(Authorization.class));
    }

    @Test
    void testDeleteAuthorization_notFound_throwsException() {
        // Arrange
        when(authorizationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authorizationService.deleteAuthorization(999L, "testuser");
        });

        assertEquals("Authorization not found: 999", exception.getMessage());
        verify(authorizationRepository, never()).delete(any(Authorization.class));
    }

    @Test
    void testSave_success_savesAuthorization() {
        // Arrange
        when(authorizationRepository.save(mockAuthorization)).thenReturn(mockAuthorization);

        // Act
        Authorization result = authorizationService.save(mockAuthorization);

        // Assert
        assertNotNull(result);
        verify(authorizationRepository, times(1)).save(mockAuthorization);
    }

    @Test
    void testRenderTemplate_basicVariables_replacesVariables() {
        // Arrange
        String template = "Authorization for {{ system_name }} on {{ date }}.";
        Map<String, String> variables = new HashMap<>();
        variables.put("system_name", "Production System");
        variables.put("date", "2024-01-15");

        // Act
        String result = authorizationService.renderTemplate(template, variables);

        // Assert
        assertEquals("Authorization for Production System on 2024-01-15.", result);
    }

    @Test
    void testRenderTemplate_nullTemplate_returnsNull() {
        // Act
        String result = authorizationService.renderTemplate(null, new HashMap<>());

        // Assert
        assertNull(result);
    }

    @Test
    void testRenderTemplate_nullVariables_returnsTemplateWithPlaceholders() {
        // Arrange
        String template = "Authorization for {{ system_name }}.";

        // Act
        String result = authorizationService.renderTemplate(template, null);

        // Assert
        assertEquals("Authorization for {{ system_name }}.", result);
    }

    @Test
    void testRenderTemplate_withUser_replacesLogoVariable() {
        // Arrange
        String template = "{{ logo }} Authorization for {{ system_name }}.";
        Map<String, String> variables = new HashMap<>();
        variables.put("system_name", "Test System");

        // Act
        String result = authorizationService.renderTemplate(template, variables, mockUser);

        // Assert
        assertTrue(result.contains("![Logo](https://example.com/logo.png)"));
        assertTrue(result.contains("Test System"));
        assertFalse(result.contains("{{ logo }}"));
        assertFalse(result.contains("{{ system_name }}"));
    }

    @Test
    void testRenderTemplate_nullUser_doesNotReplaceLogo() {
        // Arrange
        String template = "{{ logo }} Authorization.";
        Map<String, String> variables = new HashMap<>();

        // Act
        String result = authorizationService.renderTemplate(template, variables, null);

        // Assert
        assertTrue(result.contains("{{ logo }}"));
    }

    @Test
    void testRenderTemplate_userWithoutLogo_doesNotReplaceLogo() {
        // Arrange
        User userWithoutLogo = new User();
        userWithoutLogo.setUsername("user2");
        userWithoutLogo.setLogo(null);

        String template = "{{ logo }} Authorization.";
        Map<String, String> variables = new HashMap<>();

        // Act
        String result = authorizationService.renderTemplate(template, variables, userWithoutLogo);

        // Assert
        assertTrue(result.contains("{{ logo }}"));
    }

    @Test
    void testRenderTemplate_variablesWithSpaces_handlesSpaces() {
        // Arrange
        String template = "{{   system_name   }} is authorized.";
        Map<String, String> variables = new HashMap<>();
        variables.put("system_name", "Test System");

        // Act
        String result = authorizationService.renderTemplate(template, variables);

        // Assert
        assertEquals("Test System is authorized.", result);
    }

    @Test
    void testRenderTemplate_specialCharactersInValue_handlesCorrectly() {
        // Arrange
        String template = "Notes: {{ notes }}";
        Map<String, String> variables = new HashMap<>();
        variables.put("notes", "Special chars: $100 & <tag>");

        // Act
        String result = authorizationService.renderTemplate(template, variables);

        // Assert
        assertTrue(result.contains("Special chars: $100 & <tag>"));
    }
}
