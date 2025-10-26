package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.ConditionOfApproval;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConditionOfApprovalRequestTest {

    @Test
    void testNoArgsConstructor() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();

        assertNotNull(request);
        assertNull(request.getAuthorizationId());
        assertNull(request.getCondition());
        assertNull(request.getConditionType());
        assertNull(request.getDueDate());
    }

    @Test
    void testAllArgsConstructorWithMandatoryCondition() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest(
                100L,
                "Complete annual security assessment by due date",
                ConditionOfApproval.ConditionType.MANDATORY,
                "2026-06-30"
        );

        assertEquals(100L, request.getAuthorizationId());
        assertEquals("Complete annual security assessment by due date", request.getCondition());
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, request.getConditionType());
        assertEquals("2026-06-30", request.getDueDate());
    }

    @Test
    void testAllArgsConstructorWithRecommendedCondition() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest(
                200L,
                "Consider implementing automated vulnerability scanning",
                ConditionOfApproval.ConditionType.RECOMMENDED,
                null
        );

        assertEquals(200L, request.getAuthorizationId());
        assertTrue(request.getCondition().contains("automated"));
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, request.getConditionType());
        assertNull(request.getDueDate());
    }

    @Test
    void testSetAuthorizationId() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setAuthorizationId(50L);
        assertEquals(50L, request.getAuthorizationId());
    }

    @Test
    void testSetCondition() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        String condition = "Implement multi-factor authentication for all users";
        request.setCondition(condition);
        assertEquals(condition, request.getCondition());
    }

    @Test
    void testSetConditionType() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, request.getConditionType());
    }

    @Test
    void testSetDueDate() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setDueDate("2026-12-31");
        assertEquals("2026-12-31", request.getDueDate());
    }

    @Test
    void testBothConditionTypes() {
        ConditionOfApprovalRequest mandatoryRequest = new ConditionOfApprovalRequest();
        mandatoryRequest.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, mandatoryRequest.getConditionType());

        ConditionOfApprovalRequest recommendedRequest = new ConditionOfApprovalRequest();
        recommendedRequest.setConditionType(ConditionOfApproval.ConditionType.RECOMMENDED);
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, recommendedRequest.getConditionType());
    }

    @Test
    void testWithLongConditionText() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        StringBuilder longCondition = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longCondition.append("This is a detailed security requirement that must be met. ");
        }
        request.setCondition(longCondition.toString());
        assertTrue(request.getCondition().length() > 1000);
    }

    @Test
    void testWithEmptyStrings() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setCondition("");
        request.setDueDate("");

        assertEquals("", request.getCondition());
        assertEquals("", request.getDueDate());
    }

    @Test
    void testSetAllFieldsToNull() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setAuthorizationId(1L);
        request.setCondition("condition");
        request.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        request.setDueDate("2026-01-01");

        request.setAuthorizationId(null);
        request.setCondition(null);
        request.setConditionType(null);
        request.setDueDate(null);

        assertNull(request.getAuthorizationId());
        assertNull(request.getCondition());
        assertNull(request.getConditionType());
        assertNull(request.getDueDate());
    }

    @Test
    void testModifyAllFields() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();

        request.setAuthorizationId(10L);
        request.setCondition("Old condition");
        request.setConditionType(ConditionOfApproval.ConditionType.RECOMMENDED);
        request.setDueDate("2025-12-31");

        request.setAuthorizationId(20L);
        request.setCondition("New condition");
        request.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        request.setDueDate("2026-06-30");

        assertEquals(20L, request.getAuthorizationId());
        assertEquals("New condition", request.getCondition());
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, request.getConditionType());
        assertEquals("2026-06-30", request.getDueDate());
    }

    @Test
    void testWithSpecialCharactersInCondition() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        String conditionWithSpecialChars = "Must comply with NIST SP 800-53 (Rev. 5) - Controls AC-2, AC-3, & AC-6";
        request.setCondition(conditionWithSpecialChars);
        assertEquals(conditionWithSpecialChars, request.getCondition());
        assertTrue(request.getCondition().contains("&"));
        assertTrue(request.getCondition().contains("-"));
    }

    @Test
    void testWithDifferentDateFormats() {
        String[] dateFormats = {
                "2026-06-30",
                "2026-12-31",
                "2025-01-01"
        };

        for (String date : dateFormats) {
            ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
            request.setDueDate(date);
            assertEquals(date, request.getDueDate());
        }
    }

    @Test
    void testCompleteMandatoryConditionScenario() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest(
                5L,
                "Implement continuous monitoring solution for all critical systems",
                ConditionOfApproval.ConditionType.MANDATORY,
                "2026-08-01"
        );

        assertNotNull(request);
        assertEquals(5L, request.getAuthorizationId());
        assertTrue(request.getCondition().contains("continuous monitoring"));
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, request.getConditionType());
        assertEquals("2026-08-01", request.getDueDate());
    }

    @Test
    void testCompleteRecommendedConditionScenario() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest(
                7L,
                "Consider implementing automated backup procedures",
                ConditionOfApproval.ConditionType.RECOMMENDED,
                "2026-12-31"
        );

        assertNotNull(request);
        assertEquals(7L, request.getAuthorizationId());
        assertTrue(request.getCondition().toLowerCase().contains("consider"));
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, request.getConditionType());
        assertEquals("2026-12-31", request.getDueDate());
    }

    @Test
    void testMandatoryConditionWithDueDate() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setAuthorizationId(15L);
        request.setCondition("Complete penetration testing");
        request.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        request.setDueDate("2026-09-30");

        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, request.getConditionType());
        assertNotNull(request.getDueDate());
    }

    @Test
    void testRecommendedConditionWithoutDueDate() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setAuthorizationId(25L);
        request.setCondition("Review security policies quarterly");
        request.setConditionType(ConditionOfApproval.ConditionType.RECOMMENDED);
        // No due date for recommended

        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, request.getConditionType());
        assertNull(request.getDueDate());
    }

    @Test
    void testWithLargeAuthorizationId() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setAuthorizationId(999999L);
        assertEquals(999999L, request.getAuthorizationId());
    }

    @Test
    void testConditionTypeToggle() {
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();

        request.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, request.getConditionType());

        request.setConditionType(ConditionOfApproval.ConditionType.RECOMMENDED);
        assertEquals(ConditionOfApproval.ConditionType.RECOMMENDED, request.getConditionType());

        request.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        assertEquals(ConditionOfApproval.ConditionType.MANDATORY, request.getConditionType());
    }
}
