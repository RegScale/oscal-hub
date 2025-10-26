package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConditionOfApproval;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import gov.nist.oscal.tools.api.repository.ConditionOfApprovalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ConditionOfApprovalService.
 * Tests all CRUD operations for managing authorization conditions.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConditionOfApprovalServiceTest {

    @Mock
    private ConditionOfApprovalRepository conditionRepository;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @InjectMocks
    private ConditionOfApprovalService conditionOfApprovalService;

    private Authorization testAuthorization;
    private ConditionOfApproval mandatoryCondition;
    private ConditionOfApproval recommendedCondition;

    @BeforeEach
    void setUp() {
        // Create test authorization
        testAuthorization = new Authorization();
        testAuthorization.setId(1L);
        testAuthorization.setName("Test System");

        // Create mandatory condition
        mandatoryCondition = new ConditionOfApproval();
        mandatoryCondition.setId(1L);
        mandatoryCondition.setAuthorization(testAuthorization);
        mandatoryCondition.setCondition("Must implement multi-factor authentication");
        mandatoryCondition.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        mandatoryCondition.setDueDate(LocalDate.now().plusMonths(3));

        // Create recommended condition
        recommendedCondition = new ConditionOfApproval();
        recommendedCondition.setId(2L);
        recommendedCondition.setAuthorization(testAuthorization);
        recommendedCondition.setCondition("Consider implementing automated scanning");
        recommendedCondition.setConditionType(ConditionOfApproval.ConditionType.RECOMMENDED);
        recommendedCondition.setDueDate(LocalDate.now().plusMonths(6));
    }

    // ==================== Create Condition Tests ====================

    @Test
    void testCreateCondition_success() {
        when(authorizationRepository.findById(1L)).thenReturn(Optional.of(testAuthorization));
        when(conditionRepository.save(any(ConditionOfApproval.class))).thenReturn(mandatoryCondition);

        ConditionOfApproval result = conditionOfApprovalService.createCondition(
                1L,
                "Must implement multi-factor authentication",
                ConditionOfApproval.ConditionType.MANDATORY,
                LocalDate.now().plusMonths(3)
        );

        assertNotNull(result);
        assertEquals("Must implement multi-factor authentication", result.getCondition());
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, result.getConditionType());

        ArgumentCaptor<ConditionOfApproval> captor = ArgumentCaptor.forClass(ConditionOfApproval.class);
        verify(conditionRepository).save(captor.capture());

        ConditionOfApproval saved = captor.getValue();
        assertEquals(testAuthorization, saved.getAuthorization());
        assertEquals("Must implement multi-factor authentication", saved.getCondition());
    }

    @Test
    void testCreateCondition_recommendedType() {
        when(authorizationRepository.findById(1L)).thenReturn(Optional.of(testAuthorization));
        when(conditionRepository.save(any(ConditionOfApproval.class))).thenReturn(recommendedCondition);

        ConditionOfApproval result = conditionOfApprovalService.createCondition(
                1L,
                "Consider implementing automated scanning",
                ConditionOfApproval.ConditionType.RECOMMENDED,
                LocalDate.now().plusMonths(6)
        );

        assertNotNull(result);
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, result.getConditionType());
        verify(authorizationRepository).findById(1L);
        verify(conditionRepository).save(any(ConditionOfApproval.class));
    }

    @Test
    void testCreateCondition_authorizationNotFound_throwsException() {
        when(authorizationRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                conditionOfApprovalService.createCondition(
                        999L,
                        "Test condition",
                        ConditionOfApproval.ConditionType.MANDATORY,
                        LocalDate.now()
                )
        );

        assertTrue(exception.getMessage().contains("Authorization not found"));
        verify(authorizationRepository).findById(999L);
        verify(conditionRepository, never()).save(any());
    }

    @Test
    void testCreateCondition_withNullDueDate() {
        when(authorizationRepository.findById(1L)).thenReturn(Optional.of(testAuthorization));
        when(conditionRepository.save(any(ConditionOfApproval.class))).thenReturn(mandatoryCondition);

        ConditionOfApproval result = conditionOfApprovalService.createCondition(
                1L,
                "Test condition",
                ConditionOfApproval.ConditionType.MANDATORY,
                null
        );

        assertNotNull(result);
        ArgumentCaptor<ConditionOfApproval> captor = ArgumentCaptor.forClass(ConditionOfApproval.class);
        verify(conditionRepository).save(captor.capture());

        ConditionOfApproval saved = captor.getValue();
        assertNull(saved.getDueDate());
    }

    // ==================== Update Condition Tests ====================

    @Test
    void testUpdateCondition_allFieldsUpdated() {
        ConditionOfApproval existingCondition = new ConditionOfApproval();
        existingCondition.setId(1L);
        existingCondition.setCondition("Original condition");
        existingCondition.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        existingCondition.setDueDate(LocalDate.now());

        when(conditionRepository.findById(1L)).thenReturn(Optional.of(existingCondition));
        when(conditionRepository.save(any(ConditionOfApproval.class))).thenReturn(existingCondition);

        LocalDate newDueDate = LocalDate.now().plusMonths(12);
        ConditionOfApproval result = conditionOfApprovalService.updateCondition(
                1L,
                "Updated condition text",
                ConditionOfApproval.ConditionType.RECOMMENDED,
                newDueDate
        );

        assertNotNull(result);

        // Verify the entity was updated
        ArgumentCaptor<ConditionOfApproval> captor = ArgumentCaptor.forClass(ConditionOfApproval.class);
        verify(conditionRepository).save(captor.capture());

        ConditionOfApproval saved = captor.getValue();
        assertEquals("Updated condition text", saved.getCondition());
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, saved.getConditionType());
        assertEquals(newDueDate, saved.getDueDate());
    }

    @Test
    void testUpdateCondition_nullConditionNotUpdated() {
        ConditionOfApproval existingCondition = new ConditionOfApproval();
        existingCondition.setId(1L);
        existingCondition.setCondition("Original condition");
        existingCondition.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        existingCondition.setDueDate(LocalDate.now());

        when(conditionRepository.findById(1L)).thenReturn(Optional.of(existingCondition));
        when(conditionRepository.save(any(ConditionOfApproval.class))).thenReturn(existingCondition);

        conditionOfApprovalService.updateCondition(
                1L,
                null,  // Null condition should not update
                ConditionOfApproval.ConditionType.RECOMMENDED,
                LocalDate.now()
        );

        ArgumentCaptor<ConditionOfApproval> captor = ArgumentCaptor.forClass(ConditionOfApproval.class);
        verify(conditionRepository).save(captor.capture());

        ConditionOfApproval saved = captor.getValue();
        assertEquals("Original condition", saved.getCondition()); // Should remain unchanged
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, saved.getConditionType()); // Should be updated
    }

    @Test
    void testUpdateCondition_nullConditionTypeNotUpdated() {
        ConditionOfApproval existingCondition = new ConditionOfApproval();
        existingCondition.setId(1L);
        existingCondition.setCondition("Original condition");
        existingCondition.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        existingCondition.setDueDate(LocalDate.now());

        when(conditionRepository.findById(1L)).thenReturn(Optional.of(existingCondition));
        when(conditionRepository.save(any(ConditionOfApproval.class))).thenReturn(existingCondition);

        conditionOfApprovalService.updateCondition(
                1L,
                "Updated text",
                null,  // Null type should not update
                LocalDate.now()
        );

        ArgumentCaptor<ConditionOfApproval> captor = ArgumentCaptor.forClass(ConditionOfApproval.class);
        verify(conditionRepository).save(captor.capture());

        ConditionOfApproval saved = captor.getValue();
        assertEquals("Updated text", saved.getCondition()); // Should be updated
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, saved.getConditionType()); // Should remain unchanged
    }

    @Test
    void testUpdateCondition_conditionNotFound_throwsException() {
        when(conditionRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                conditionOfApprovalService.updateCondition(
                        999L,
                        "Updated text",
                        ConditionOfApproval.ConditionType.MANDATORY,
                        LocalDate.now()
                )
        );

        assertTrue(exception.getMessage().contains("Condition not found"));
        verify(conditionRepository, never()).save(any());
    }

    // ==================== Get Condition Tests ====================

    @Test
    void testGetCondition_found() {
        when(conditionRepository.findById(1L)).thenReturn(Optional.of(mandatoryCondition));

        ConditionOfApproval result = conditionOfApprovalService.getCondition(1L);

        assertNotNull(result);
        assertEquals(mandatoryCondition.getId(), result.getId());
        assertEquals(mandatoryCondition.getCondition(), result.getCondition());
        verify(conditionRepository).findById(1L);
    }

    @Test
    void testGetCondition_notFound_throwsException() {
        when(conditionRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                conditionOfApprovalService.getCondition(999L)
        );

        assertTrue(exception.getMessage().contains("Condition not found"));
    }

    // ==================== Get Conditions By Authorization Tests ====================

    @Test
    void testGetConditionsByAuthorization_returnsMultiple() {
        List<ConditionOfApproval> conditions = Arrays.asList(mandatoryCondition, recommendedCondition);
        when(conditionRepository.findByAuthorizationId(1L)).thenReturn(conditions);

        List<ConditionOfApproval> result = conditionOfApprovalService.getConditionsByAuthorization(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(mandatoryCondition));
        assertTrue(result.contains(recommendedCondition));
        verify(conditionRepository).findByAuthorizationId(1L);
    }

    @Test
    void testGetConditionsByAuthorization_emptyList() {
        when(conditionRepository.findByAuthorizationId(999L)).thenReturn(Collections.emptyList());

        List<ConditionOfApproval> result = conditionOfApprovalService.getConditionsByAuthorization(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Get Conditions By Authorization And Type Tests ====================

    @Test
    void testGetConditionsByAuthorizationAndType_mandatory() {
        when(conditionRepository.findByAuthorizationIdAndConditionType(
                1L, ConditionOfApproval.ConditionType.MANDATORY))
                .thenReturn(Arrays.asList(mandatoryCondition));

        List<ConditionOfApproval> result = conditionOfApprovalService.getConditionsByAuthorizationAndType(
                1L, ConditionOfApproval.ConditionType.MANDATORY);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, result.get(0).getConditionType());
        verify(conditionRepository).findByAuthorizationIdAndConditionType(
                1L, ConditionOfApproval.ConditionType.MANDATORY);
    }

    @Test
    void testGetConditionsByAuthorizationAndType_recommended() {
        when(conditionRepository.findByAuthorizationIdAndConditionType(
                1L, ConditionOfApproval.ConditionType.RECOMMENDED))
                .thenReturn(Arrays.asList(recommendedCondition));

        List<ConditionOfApproval> result = conditionOfApprovalService.getConditionsByAuthorizationAndType(
                1L, ConditionOfApproval.ConditionType.RECOMMENDED);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, result.get(0).getConditionType());
    }

    @Test
    void testGetConditionsByAuthorizationAndType_noMatches() {
        when(conditionRepository.findByAuthorizationIdAndConditionType(
                999L, ConditionOfApproval.ConditionType.MANDATORY))
                .thenReturn(Collections.emptyList());

        List<ConditionOfApproval> result = conditionOfApprovalService.getConditionsByAuthorizationAndType(
                999L, ConditionOfApproval.ConditionType.MANDATORY);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Delete Condition Tests ====================

    @Test
    void testDeleteCondition_success() {
        when(conditionRepository.findById(1L)).thenReturn(Optional.of(mandatoryCondition));
        doNothing().when(conditionRepository).delete(any(ConditionOfApproval.class));

        conditionOfApprovalService.deleteCondition(1L);

        verify(conditionRepository).findById(1L);
        verify(conditionRepository).delete(mandatoryCondition);
    }

    @Test
    void testDeleteCondition_notFound_throwsException() {
        when(conditionRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                conditionOfApprovalService.deleteCondition(999L)
        );

        assertTrue(exception.getMessage().contains("Condition not found"));
        verify(conditionRepository, never()).delete(any());
    }

    // ==================== Delete Conditions By Authorization Tests ====================

    @Test
    void testDeleteConditionsByAuthorization_multipleConditions() {
        List<ConditionOfApproval> conditions = Arrays.asList(mandatoryCondition, recommendedCondition);
        when(conditionRepository.findByAuthorizationId(1L)).thenReturn(conditions);
        doNothing().when(conditionRepository).deleteAll(anyList());

        conditionOfApprovalService.deleteConditionsByAuthorization(1L);

        verify(conditionRepository).findByAuthorizationId(1L);
        verify(conditionRepository).deleteAll(conditions);
    }

    @Test
    void testDeleteConditionsByAuthorization_noConditions() {
        when(conditionRepository.findByAuthorizationId(999L)).thenReturn(Collections.emptyList());
        doNothing().when(conditionRepository).deleteAll(anyList());

        conditionOfApprovalService.deleteConditionsByAuthorization(999L);

        verify(conditionRepository).findByAuthorizationId(999L);
        verify(conditionRepository).deleteAll(Collections.emptyList());
    }

    @Test
    void testDeleteConditionsByAuthorization_singleCondition() {
        when(conditionRepository.findByAuthorizationId(1L))
                .thenReturn(Arrays.asList(mandatoryCondition));
        doNothing().when(conditionRepository).deleteAll(anyList());

        conditionOfApprovalService.deleteConditionsByAuthorization(1L);

        ArgumentCaptor<List<ConditionOfApproval>> captor = ArgumentCaptor.forClass(List.class);
        verify(conditionRepository).deleteAll(captor.capture());

        List<ConditionOfApproval> deletedConditions = captor.getValue();
        assertEquals(1, deletedConditions.size());
        assertEquals(mandatoryCondition, deletedConditions.get(0));
    }
}
