package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConditionOfApproval;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConditionOfApprovalResponseTest {

    @Test
    void testNoArgsConstructor() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();

        assertNotNull(response);
        assertNull(response.getId());
        assertNull(response.getAuthorizationId());
        assertNull(response.getCondition());
        assertNull(response.getConditionType());
        assertNull(response.getDueDate());
        assertNull(response.getCreatedAt());
        assertNull(response.getUpdatedAt());
    }

    @Test
    void testEntityConstructorWithMandatoryCondition() {
        // Create mock authorization
        Authorization authorization = mock(Authorization.class);
        when(authorization.getId()).thenReturn(1L);

        // Create mock condition
        ConditionOfApproval condition = mock(ConditionOfApproval.class);
        when(condition.getId()).thenReturn(10L);
        when(condition.getAuthorization()).thenReturn(authorization);
        when(condition.getCondition()).thenReturn("Must complete security assessment");
        when(condition.getConditionType()).thenReturn(ConditionOfApproval.ConditionType.MANDATORY);
        when(condition.getDueDate()).thenReturn(LocalDate.of(2025, 6, 30));
        when(condition.getCreatedAt()).thenReturn(LocalDateTime.of(2025, 1, 1, 10, 0));
        when(condition.getUpdatedAt()).thenReturn(LocalDateTime.of(2025, 1, 15, 14, 30));

        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse(condition);

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getAuthorizationId());
        assertEquals("Must complete security assessment", response.getCondition());
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, response.getConditionType());
        assertEquals(LocalDate.of(2025, 6, 30), response.getDueDate());
        assertEquals(LocalDateTime.of(2025, 1, 1, 10, 0), response.getCreatedAt());
        assertEquals(LocalDateTime.of(2025, 1, 15, 14, 30), response.getUpdatedAt());
    }

    @Test
    void testEntityConstructorWithRecommendedCondition() {
        Authorization authorization = mock(Authorization.class);
        when(authorization.getId()).thenReturn(2L);

        ConditionOfApproval condition = mock(ConditionOfApproval.class);
        when(condition.getId()).thenReturn(20L);
        when(condition.getAuthorization()).thenReturn(authorization);
        when(condition.getCondition()).thenReturn("Recommended to implement MFA");
        when(condition.getConditionType()).thenReturn(ConditionOfApproval.ConditionType.RECOMMENDED);
        when(condition.getDueDate()).thenReturn(LocalDate.of(2025, 12, 31));
        when(condition.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(condition.getUpdatedAt()).thenReturn(LocalDateTime.now());

        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse(condition);

        assertEquals(20L, response.getId());
        assertEquals(2L, response.getAuthorizationId());
        assertTrue(response.getCondition().contains("MFA"));
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, response.getConditionType());
        assertNotNull(response.getDueDate());
    }

    @Test
    void testSetId() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        assertNull(response.getId());

        response.setId(100L);
        assertEquals(100L, response.getId());
    }

    @Test
    void testSetAuthorizationId() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        assertNull(response.getAuthorizationId());

        response.setAuthorizationId(50L);
        assertEquals(50L, response.getAuthorizationId());
    }

    @Test
    void testSetCondition() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        assertNull(response.getCondition());

        String condition = "Update security controls quarterly";
        response.setCondition(condition);
        assertEquals(condition, response.getCondition());
    }

    @Test
    void testSetConditionType() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        assertNull(response.getConditionType());

        response.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, response.getConditionType());

        response.setConditionType(ConditionOfApproval.ConditionType.RECOMMENDED);
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, response.getConditionType());
    }

    @Test
    void testSetDueDate() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        assertNull(response.getDueDate());

        LocalDate dueDate = LocalDate.of(2025, 9, 15);
        response.setDueDate(dueDate);
        assertEquals(dueDate, response.getDueDate());
    }

    @Test
    void testSetCreatedAt() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        assertNull(response.getCreatedAt());

        LocalDateTime createdAt = LocalDateTime.of(2025, 2, 1, 9, 0);
        response.setCreatedAt(createdAt);
        assertEquals(createdAt, response.getCreatedAt());
    }

    @Test
    void testSetUpdatedAt() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        assertNull(response.getUpdatedAt());

        LocalDateTime updatedAt = LocalDateTime.of(2025, 3, 1, 16, 45);
        response.setUpdatedAt(updatedAt);
        assertEquals(updatedAt, response.getUpdatedAt());
    }

    @Test
    void testWithLongConditionText() {
        StringBuilder longCondition = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longCondition.append("This is a very detailed condition that must be met. ");
        }

        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        response.setCondition(longCondition.toString());

        assertTrue(response.getCondition().length() > 1000);
    }

    @Test
    void testSetAllFieldsToNull() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        response.setId(1L);
        response.setAuthorizationId(2L);
        response.setCondition("Condition");
        response.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        response.setDueDate(LocalDate.now());
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());

        response.setId(null);
        response.setAuthorizationId(null);
        response.setCondition(null);
        response.setConditionType(null);
        response.setDueDate(null);
        response.setCreatedAt(null);
        response.setUpdatedAt(null);

        assertNull(response.getId());
        assertNull(response.getAuthorizationId());
        assertNull(response.getCondition());
        assertNull(response.getConditionType());
        assertNull(response.getDueDate());
        assertNull(response.getCreatedAt());
        assertNull(response.getUpdatedAt());
    }

    @Test
    void testModifyAllFields() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();

        response.setId(1L);
        response.setAuthorizationId(10L);
        response.setCondition("Old Condition");
        response.setConditionType(ConditionOfApproval.ConditionType.RECOMMENDED);
        response.setDueDate(LocalDate.of(2025, 1, 1));
        response.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        response.setUpdatedAt(LocalDateTime.of(2024, 6, 1, 0, 0));

        response.setId(2L);
        response.setAuthorizationId(20L);
        response.setCondition("New Condition");
        response.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        response.setDueDate(LocalDate.of(2025, 12, 31));
        response.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        response.setUpdatedAt(LocalDateTime.of(2025, 6, 1, 0, 0));

        assertEquals(2L, response.getId());
        assertEquals(20L, response.getAuthorizationId());
        assertEquals("New Condition", response.getCondition());
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, response.getConditionType());
        assertEquals(LocalDate.of(2025, 12, 31), response.getDueDate());
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), response.getCreatedAt());
        assertEquals(LocalDateTime.of(2025, 6, 1, 0, 0), response.getUpdatedAt());
    }

    @Test
    void testWithEmptyConditionString() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        response.setCondition("");
        assertEquals("", response.getCondition());
    }

    @Test
    void testWithPastDueDate() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        LocalDate pastDate = LocalDate.of(2020, 1, 1);
        response.setDueDate(pastDate);
        assertEquals(pastDate, response.getDueDate());
        assertTrue(response.getDueDate().isBefore(LocalDate.now()));
    }

    @Test
    void testWithFutureDueDate() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        LocalDate futureDate = LocalDate.of(2030, 12, 31);
        response.setDueDate(futureDate);
        assertEquals(futureDate, response.getDueDate());
        assertTrue(response.getDueDate().isAfter(LocalDate.now()));
    }

    @Test
    void testCompleteMandatoryConditionScenario() {
        // Create a complete mandatory condition scenario
        Authorization authorization = mock(Authorization.class);
        when(authorization.getId()).thenReturn(5L);

        ConditionOfApproval condition = mock(ConditionOfApproval.class);
        when(condition.getId()).thenReturn(15L);
        when(condition.getAuthorization()).thenReturn(authorization);
        when(condition.getCondition()).thenReturn("Implement continuous monitoring solution");
        when(condition.getConditionType()).thenReturn(ConditionOfApproval.ConditionType.MANDATORY);
        when(condition.getDueDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(condition.getCreatedAt()).thenReturn(LocalDateTime.of(2025, 1, 15, 10, 30));
        when(condition.getUpdatedAt()).thenReturn(LocalDateTime.of(2025, 1, 20, 11, 45));

        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse(condition);

        // Verify mandatory condition properties
        assertNotNull(response);
        assertEquals(15L, response.getId());
        assertEquals(5L, response.getAuthorizationId());
        assertTrue(response.getCondition().contains("monitoring"));
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, response.getConditionType());
        assertTrue(response.getDueDate().isAfter(LocalDate.now()));
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
        assertTrue(response.getUpdatedAt().isAfter(response.getCreatedAt()));
    }

    @Test
    void testCompleteRecommendedConditionScenario() {
        // Create a complete recommended condition scenario
        Authorization authorization = mock(Authorization.class);
        when(authorization.getId()).thenReturn(7L);

        ConditionOfApproval condition = mock(ConditionOfApproval.class);
        when(condition.getId()).thenReturn(25L);
        when(condition.getAuthorization()).thenReturn(authorization);
        when(condition.getCondition()).thenReturn("Consider implementing automated backup procedures");
        when(condition.getConditionType()).thenReturn(ConditionOfApproval.ConditionType.RECOMMENDED);
        when(condition.getDueDate()).thenReturn(LocalDate.of(2025, 12, 31));
        when(condition.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(condition.getUpdatedAt()).thenReturn(LocalDateTime.now());

        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse(condition);

        // Verify recommended condition properties
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, response.getConditionType());
        assertTrue(response.getCondition().toLowerCase().contains("consider"));
        assertNotNull(response.getDueDate());
    }

    @Test
    void testConditionTypeToggle() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();

        // Start as MANDATORY
        response.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, response.getConditionType());

        // Change to RECOMMENDED
        response.setConditionType(ConditionOfApproval.ConditionType.RECOMMENDED);
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, response.getConditionType());

        // Change back to MANDATORY
        response.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, response.getConditionType());
    }

    @Test
    void testWithSpecialCharactersInCondition() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        String conditionWithSpecialChars = "Condition: Must comply with NIST SP 800-53 (Rev. 5) - Controls AC-2, AC-3, & AC-6";
        response.setCondition(conditionWithSpecialChars);

        assertEquals(conditionWithSpecialChars, response.getCondition());
        assertTrue(response.getCondition().contains("&"));
        assertTrue(response.getCondition().contains("-"));
    }

    @Test
    void testWithTodayAsDueDate() {
        ConditionOfApprovalResponse response = new ConditionOfApprovalResponse();
        LocalDate today = LocalDate.now();
        response.setDueDate(today);
        assertEquals(today, response.getDueDate());
    }
}
