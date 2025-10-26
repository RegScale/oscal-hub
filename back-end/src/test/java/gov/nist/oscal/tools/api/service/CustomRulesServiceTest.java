package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import gov.nist.oscal.tools.api.model.CustomRuleRequest;
import gov.nist.oscal.tools.api.model.CustomRuleResponse;
import gov.nist.oscal.tools.api.repository.CustomValidationRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for CustomRulesService.
 * Tests all CRUD operations for custom validation rules.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomRulesServiceTest {

    @Mock
    private CustomValidationRuleRepository repository;

    @InjectMocks
    private CustomRulesService customRulesService;

    private CustomValidationRule testRule1;
    private CustomValidationRule testRule2;
    private CustomRuleRequest testRequest;

    @BeforeEach
    void setUp() {
        // Create test rule 1 (enabled)
        testRule1 = new CustomValidationRule();
        testRule1.setId(1L);
        testRule1.setRuleId("RULE-001");
        testRule1.setName("Test Rule 1");
        testRule1.setDescription("Test description 1");
        testRule1.setRuleType("JSON_SCHEMA");
        testRule1.setSeverity("ERROR");
        testRule1.setCategory("Security");
        testRule1.setFieldPath("$.metadata.title");
        testRule1.setRuleExpression("regex(.*)");
        testRule1.setApplicableModelTypes("catalog,profile");
        testRule1.setEnabled(true);

        // Create test rule 2 (disabled)
        testRule2 = new CustomValidationRule();
        testRule2.setId(2L);
        testRule2.setRuleId("RULE-002");
        testRule2.setName("Test Rule 2");
        testRule2.setDescription("Test description 2");
        testRule2.setRuleType("XPATH");
        testRule2.setSeverity("WARNING");
        testRule2.setCategory("Format");
        testRule2.setFieldPath("//metadata");
        testRule2.setApplicableModelTypes("ssp");
        testRule2.setEnabled(false);

        // Create test request
        testRequest = new CustomRuleRequest();
        testRequest.setRuleId("RULE-003");
        testRequest.setName("New Rule");
        testRequest.setDescription("New rule description");
        testRequest.setRuleType("JSON_SCHEMA");
        testRequest.setSeverity("ERROR");
        testRequest.setCategory("Compliance");
        testRequest.setFieldPath("$.metadata");
        testRequest.setRuleExpression("required");
        testRequest.setApplicableModelTypes(Arrays.asList("catalog", "profile"));
        testRequest.setEnabled(true);
    }

    // ==================== Get All Custom Rules Tests ====================

    @Test
    void testGetAllCustomRules_returnsMultiple() {
        when(repository.findAll()).thenReturn(Arrays.asList(testRule1, testRule2));

        List<CustomRuleResponse> result = customRulesService.getAllCustomRules();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAll();
    }

    @Test
    void testGetAllCustomRules_empty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<CustomRuleResponse> result = customRulesService.getAllCustomRules();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Get Custom Rule By ID Tests ====================

    @Test
    void testGetCustomRuleById_found() {
        when(repository.findById(1L)).thenReturn(Optional.of(testRule1));

        Optional<CustomRuleResponse> result = customRulesService.getCustomRuleById(1L);

        assertTrue(result.isPresent());
        verify(repository).findById(1L);
    }

    @Test
    void testGetCustomRuleById_notFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        Optional<CustomRuleResponse> result = customRulesService.getCustomRuleById(999L);

        assertFalse(result.isPresent());
    }

    // ==================== Get Custom Rule By RuleId Tests ====================

    @Test
    void testGetCustomRuleByRuleId_found() {
        when(repository.findByRuleId("RULE-001")).thenReturn(Optional.of(testRule1));

        Optional<CustomRuleResponse> result = customRulesService.getCustomRuleByRuleId("RULE-001");

        assertTrue(result.isPresent());
        verify(repository).findByRuleId("RULE-001");
    }

    @Test
    void testGetCustomRuleByRuleId_notFound() {
        when(repository.findByRuleId("NONEXISTENT")).thenReturn(Optional.empty());

        Optional<CustomRuleResponse> result = customRulesService.getCustomRuleByRuleId("NONEXISTENT");

        assertFalse(result.isPresent());
    }

    // ==================== Get Enabled Custom Rules Tests ====================

    @Test
    void testGetEnabledCustomRules_returnsOnlyEnabled() {
        when(repository.findByEnabledTrue()).thenReturn(Arrays.asList(testRule1));

        List<CustomRuleResponse> result = customRulesService.getEnabledCustomRules();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByEnabledTrue();
    }

    @Test
    void testGetEnabledCustomRules_empty() {
        when(repository.findByEnabledTrue()).thenReturn(Collections.emptyList());

        List<CustomRuleResponse> result = customRulesService.getEnabledCustomRules();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Create Custom Rule Tests ====================

    @Test
    void testCreateCustomRule_success() {
        when(repository.existsByRuleId("RULE-003")).thenReturn(false);
        when(repository.save(any(CustomValidationRule.class))).thenAnswer(invocation -> {
            CustomValidationRule saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        CustomRuleResponse result = customRulesService.createCustomRule(testRequest);

        assertNotNull(result);
        verify(repository).existsByRuleId("RULE-003");

        ArgumentCaptor<CustomValidationRule> captor = ArgumentCaptor.forClass(CustomValidationRule.class);
        verify(repository).save(captor.capture());

        CustomValidationRule saved = captor.getValue();
        assertEquals("RULE-003", saved.getRuleId());
        assertEquals("New Rule", saved.getName());
        assertEquals("catalog,profile", saved.getApplicableModelTypes());
        assertTrue(saved.getEnabled());
    }

    @Test
    void testCreateCustomRule_duplicateRuleId_throwsException() {
        when(repository.existsByRuleId("RULE-003")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                customRulesService.createCustomRule(testRequest)
        );

        assertTrue(exception.getMessage().contains("Rule ID already exists"));
        verify(repository, never()).save(any());
    }

    @Test
    void testCreateCustomRule_nullApplicableModelTypes() {
        testRequest.setApplicableModelTypes(null);
        when(repository.existsByRuleId("RULE-003")).thenReturn(false);
        when(repository.save(any(CustomValidationRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customRulesService.createCustomRule(testRequest);

        ArgumentCaptor<CustomValidationRule> captor = ArgumentCaptor.forClass(CustomValidationRule.class);
        verify(repository).save(captor.capture());

        CustomValidationRule saved = captor.getValue();
        assertNull(saved.getApplicableModelTypes());
    }

    @Test
    void testCreateCustomRule_emptyApplicableModelTypes() {
        testRequest.setApplicableModelTypes(Collections.emptyList());
        when(repository.existsByRuleId("RULE-003")).thenReturn(false);
        when(repository.save(any(CustomValidationRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customRulesService.createCustomRule(testRequest);

        ArgumentCaptor<CustomValidationRule> captor = ArgumentCaptor.forClass(CustomValidationRule.class);
        verify(repository).save(captor.capture());

        CustomValidationRule saved = captor.getValue();
        assertNull(saved.getApplicableModelTypes());
    }

    // ==================== Update Custom Rule Tests ====================

    @Test
    void testUpdateCustomRule_success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testRule1));
        when(repository.save(any(CustomValidationRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        testRequest.setRuleId("RULE-001"); // Same ID, no conflict

        CustomRuleResponse result = customRulesService.updateCustomRule(1L, testRequest);

        assertNotNull(result);

        ArgumentCaptor<CustomValidationRule> captor = ArgumentCaptor.forClass(CustomValidationRule.class);
        verify(repository).save(captor.capture());

        CustomValidationRule updated = captor.getValue();
        assertEquals("New Rule", updated.getName());
        assertEquals("catalog,profile", updated.getApplicableModelTypes());
    }

    @Test
    void testUpdateCustomRule_changingRuleIdToExisting_throwsException() {
        when(repository.findById(1L)).thenReturn(Optional.of(testRule1));
        when(repository.existsByRuleId("RULE-002")).thenReturn(true);

        testRequest.setRuleId("RULE-002"); // Trying to change to existing ID

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                customRulesService.updateCustomRule(1L, testRequest)
        );

        assertTrue(exception.getMessage().contains("Rule ID already exists"));
        verify(repository, never()).save(any());
    }

    @Test
    void testUpdateCustomRule_changingRuleIdToNew_success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testRule1));
        when(repository.existsByRuleId("RULE-NEW")).thenReturn(false);
        when(repository.save(any(CustomValidationRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        testRequest.setRuleId("RULE-NEW");

        CustomRuleResponse result = customRulesService.updateCustomRule(1L, testRequest);

        assertNotNull(result);
        verify(repository).existsByRuleId("RULE-NEW");
        verify(repository).save(testRule1);
    }

    @Test
    void testUpdateCustomRule_notFound_throwsException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                customRulesService.updateCustomRule(999L, testRequest)
        );

        assertTrue(exception.getMessage().contains("Custom rule not found"));
        verify(repository, never()).save(any());
    }

    // ==================== Delete Custom Rule Tests ====================

    @Test
    void testDeleteCustomRule_success() {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        customRulesService.deleteCustomRule(1L);

        verify(repository).existsById(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void testDeleteCustomRule_notFound_throwsException() {
        when(repository.existsById(999L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                customRulesService.deleteCustomRule(999L)
        );

        assertTrue(exception.getMessage().contains("Custom rule not found"));
        verify(repository, never()).deleteById(any());
    }

    // ==================== Toggle Rule Enabled Tests ====================

    @Test
    void testToggleRuleEnabled_enabledToDisabled() {
        testRule1.setEnabled(true);
        when(repository.findById(1L)).thenReturn(Optional.of(testRule1));
        when(repository.save(any(CustomValidationRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomRuleResponse result = customRulesService.toggleRuleEnabled(1L);

        assertNotNull(result);

        ArgumentCaptor<CustomValidationRule> captor = ArgumentCaptor.forClass(CustomValidationRule.class);
        verify(repository).save(captor.capture());

        CustomValidationRule toggled = captor.getValue();
        assertFalse(toggled.getEnabled());
    }

    @Test
    void testToggleRuleEnabled_disabledToEnabled() {
        testRule2.setEnabled(false);
        when(repository.findById(2L)).thenReturn(Optional.of(testRule2));
        when(repository.save(any(CustomValidationRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomRuleResponse result = customRulesService.toggleRuleEnabled(2L);

        assertNotNull(result);

        ArgumentCaptor<CustomValidationRule> captor = ArgumentCaptor.forClass(CustomValidationRule.class);
        verify(repository).save(captor.capture());

        CustomValidationRule toggled = captor.getValue();
        assertTrue(toggled.getEnabled());
    }

    @Test
    void testToggleRuleEnabled_notFound_throwsException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                customRulesService.toggleRuleEnabled(999L)
        );

        assertTrue(exception.getMessage().contains("Custom rule not found"));
        verify(repository, never()).save(any());
    }

    // ==================== Get Custom Rules By Category Tests ====================

    @Test
    void testGetCustomRulesByCategory_found() {
        when(repository.findByCategory("Security")).thenReturn(Arrays.asList(testRule1));

        List<CustomRuleResponse> result = customRulesService.getCustomRulesByCategory("Security");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByCategory("Security");
    }

    @Test
    void testGetCustomRulesByCategory_empty() {
        when(repository.findByCategory("Nonexistent")).thenReturn(Collections.emptyList());

        List<CustomRuleResponse> result = customRulesService.getCustomRulesByCategory("Nonexistent");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Get Custom Rules For Model Type Tests ====================

    @Test
    void testGetCustomRulesForModelType_found() {
        when(repository.findEnabledRulesForModelType("catalog")).thenReturn(Arrays.asList(testRule1));

        List<CustomRuleResponse> result = customRulesService.getCustomRulesForModelType("catalog");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findEnabledRulesForModelType("catalog");
    }

    @Test
    void testGetCustomRulesForModelType_empty() {
        when(repository.findEnabledRulesForModelType("poam")).thenReturn(Collections.emptyList());

        List<CustomRuleResponse> result = customRulesService.getCustomRulesForModelType("poam");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
