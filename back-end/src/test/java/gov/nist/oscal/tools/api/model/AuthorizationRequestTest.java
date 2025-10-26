package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationRequestTest {

    @Test
    void testNoArgsConstructor() {
        AuthorizationRequest request = new AuthorizationRequest();

        assertNotNull(request);
        assertNull(request.getName());
        assertNull(request.getSspItemId());
        assertNull(request.getSarItemId());
        assertNull(request.getTemplateId());
        assertNull(request.getVariableValues());
        assertNull(request.getDateAuthorized());
        assertNull(request.getDateExpired());
        assertNull(request.getSystemOwner());
        assertNull(request.getSecurityManager());
        assertNull(request.getAuthorizingOfficial());
        assertNull(request.getEditedContent());
        assertNotNull(request.getConditions());
        assertEquals(0, request.getConditions().size());
    }

    @Test
    void testFourArgsConstructor() {
        Map<String, String> variableValues = new HashMap<>();
        variableValues.put("systemName", "Production System");
        variableValues.put("environment", "Production");

        AuthorizationRequest request = new AuthorizationRequest(
                "Production ATO",
                "ssp-item-123",
                1L,
                variableValues
        );

        assertEquals("Production ATO", request.getName());
        assertEquals("ssp-item-123", request.getSspItemId());
        assertEquals(1L, request.getTemplateId());
        assertEquals(2, request.getVariableValues().size());
        assertTrue(request.getVariableValues().containsKey("systemName"));
    }

    @Test
    void testSetName() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setName("Development ATO");
        assertEquals("Development ATO", request.getName());
    }

    @Test
    void testSetSspItemId() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setSspItemId("ssp-456");
        assertEquals("ssp-456", request.getSspItemId());
    }

    @Test
    void testSetSarItemId() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setSarItemId("sar-789");
        assertEquals("sar-789", request.getSarItemId());
    }

    @Test
    void testSetTemplateId() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setTemplateId(5L);
        assertEquals(5L, request.getTemplateId());
    }

    @Test
    void testSetVariableValues() {
        AuthorizationRequest request = new AuthorizationRequest();
        Map<String, String> variables = new HashMap<>();
        variables.put("var1", "value1");
        variables.put("var2", "value2");
        request.setVariableValues(variables);

        assertEquals(2, request.getVariableValues().size());
        assertEquals("value1", request.getVariableValues().get("var1"));
    }

    @Test
    void testSetDateAuthorized() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setDateAuthorized("2025-01-15");
        assertEquals("2025-01-15", request.getDateAuthorized());
    }

    @Test
    void testSetDateExpired() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setDateExpired("2028-01-15");
        assertEquals("2028-01-15", request.getDateExpired());
    }

    @Test
    void testSetSystemOwner() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setSystemOwner("John Doe");
        assertEquals("John Doe", request.getSystemOwner());
    }

    @Test
    void testSetSecurityManager() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setSecurityManager("Jane Smith");
        assertEquals("Jane Smith", request.getSecurityManager());
    }

    @Test
    void testSetAuthorizingOfficial() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setAuthorizingOfficial("Robert Johnson");
        assertEquals("Robert Johnson", request.getAuthorizingOfficial());
    }

    @Test
    void testSetEditedContent() {
        AuthorizationRequest request = new AuthorizationRequest();
        String editedContent = "This is custom authorization letter content.";
        request.setEditedContent(editedContent);
        assertEquals(editedContent, request.getEditedContent());
    }

    @Test
    void testSetConditions() {
        AuthorizationRequest request = new AuthorizationRequest();

        ConditionOfApprovalRequest condition1 = new ConditionOfApprovalRequest();
        condition1.setCondition("Complete security assessment");

        ConditionOfApprovalRequest condition2 = new ConditionOfApprovalRequest();
        condition2.setCondition("Implement MFA");

        List<ConditionOfApprovalRequest> conditions = Arrays.asList(condition1, condition2);
        request.setConditions(conditions);

        assertEquals(2, request.getConditions().size());
        assertEquals("Complete security assessment", request.getConditions().get(0).getCondition());
    }

    @Test
    void testSetAllFields() {
        Map<String, String> variables = new HashMap<>();
        variables.put("systemName", "Critical System");
        variables.put("classifiction", "High");

        ConditionOfApprovalRequest condition = new ConditionOfApprovalRequest();
        condition.setCondition("Annual review required");

        AuthorizationRequest request = new AuthorizationRequest();
        request.setName("High Impact System ATO");
        request.setSspItemId("ssp-critical-001");
        request.setSarItemId("sar-critical-001");
        request.setTemplateId(10L);
        request.setVariableValues(variables);
        request.setDateAuthorized("2025-02-01");
        request.setDateExpired("2028-02-01");
        request.setSystemOwner("Alice Anderson");
        request.setSecurityManager("Bob Brown");
        request.setAuthorizingOfficial("Charlie Chen");
        request.setEditedContent("Custom authorization letter for critical system");
        request.setConditions(Collections.singletonList(condition));

        assertEquals("High Impact System ATO", request.getName());
        assertEquals("ssp-critical-001", request.getSspItemId());
        assertEquals("sar-critical-001", request.getSarItemId());
        assertEquals(10L, request.getTemplateId());
        assertEquals(2, request.getVariableValues().size());
        assertEquals("2025-02-01", request.getDateAuthorized());
        assertEquals("2028-02-01", request.getDateExpired());
        assertEquals("Alice Anderson", request.getSystemOwner());
        assertEquals("Bob Brown", request.getSecurityManager());
        assertEquals("Charlie Chen", request.getAuthorizingOfficial());
        assertNotNull(request.getEditedContent());
        assertEquals(1, request.getConditions().size());
    }

    @Test
    void testSetFieldsToNull() {
        Map<String, String> variables = new HashMap<>();
        variables.put("key", "value");

        AuthorizationRequest request = new AuthorizationRequest(
                "Test ATO",
                "ssp-123",
                1L,
                variables
        );

        request.setName(null);
        request.setSspItemId(null);
        request.setSarItemId(null);
        request.setTemplateId(null);
        request.setVariableValues(null);
        request.setDateAuthorized(null);
        request.setDateExpired(null);
        request.setSystemOwner(null);
        request.setSecurityManager(null);
        request.setAuthorizingOfficial(null);
        request.setEditedContent(null);
        request.setConditions(null);

        assertNull(request.getName());
        assertNull(request.getSspItemId());
        assertNull(request.getSarItemId());
        assertNull(request.getTemplateId());
        assertNull(request.getVariableValues());
        assertNull(request.getDateAuthorized());
        assertNull(request.getDateExpired());
        assertNull(request.getSystemOwner());
        assertNull(request.getSecurityManager());
        assertNull(request.getAuthorizingOfficial());
        assertNull(request.getEditedContent());
        assertNull(request.getConditions());
    }

    @Test
    void testModifyAllFields() {
        AuthorizationRequest request = new AuthorizationRequest();

        request.setName("Old Name");
        request.setSystemOwner("Old Owner");
        request.setTemplateId(1L);

        request.setName("New Name");
        request.setSystemOwner("New Owner");
        request.setTemplateId(2L);

        assertEquals("New Name", request.getName());
        assertEquals("New Owner", request.getSystemOwner());
        assertEquals(2L, request.getTemplateId());
    }

    @Test
    void testWithEmptyVariableValues() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setVariableValues(new HashMap<>());

        assertNotNull(request.getVariableValues());
        assertEquals(0, request.getVariableValues().size());
    }

    @Test
    void testWithManyVariableValues() {
        AuthorizationRequest request = new AuthorizationRequest();
        Map<String, String> variables = new HashMap<>();
        for (int i = 1; i <= 20; i++) {
            variables.put("variable" + i, "value" + i);
        }
        request.setVariableValues(variables);

        assertEquals(20, request.getVariableValues().size());
        assertTrue(request.getVariableValues().containsKey("variable10"));
    }

    @Test
    void testWithEmptyConditions() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setConditions(new ArrayList<>());

        assertNotNull(request.getConditions());
        assertEquals(0, request.getConditions().size());
    }

    @Test
    void testWithManyConditions() {
        AuthorizationRequest request = new AuthorizationRequest();
        List<ConditionOfApprovalRequest> conditions = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            ConditionOfApprovalRequest condition = new ConditionOfApprovalRequest();
            condition.setCondition("Condition " + i);
            conditions.add(condition);
        }

        request.setConditions(conditions);
        assertEquals(10, request.getConditions().size());
    }

    @Test
    void testWithLongEditedContent() {
        AuthorizationRequest request = new AuthorizationRequest();
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longContent.append("This is a detailed authorization letter paragraph. ");
        }
        request.setEditedContent(longContent.toString());
        assertTrue(request.getEditedContent().length() > 1000);
    }

    @Test
    void testCompleteAuthorizationScenario() {
        Map<String, String> variables = new HashMap<>();
        variables.put("systemName", "Enterprise Resource Planning System");
        variables.put("environment", "Production");
        variables.put("classification", "Moderate");
        variables.put("owner", "IT Department");

        ConditionOfApprovalRequest condition1 = new ConditionOfApprovalRequest();
        condition1.setCondition("Complete annual security assessment by December 31, 2025");
        condition1.setDueDate("2025-12-31");

        ConditionOfApprovalRequest condition2 = new ConditionOfApprovalRequest();
        condition2.setCondition("Implement continuous monitoring solution within 90 days");
        condition2.setDueDate("2025-05-01");

        AuthorizationRequest request = new AuthorizationRequest(
                "ERP Production System ATO - FY2025",
                "ssp-erp-prod-2025",
                5L,
                variables
        );

        request.setSarItemId("sar-erp-prod-2025");
        request.setDateAuthorized("2025-02-01");
        request.setDateExpired("2028-01-31");
        request.setSystemOwner("Michael Williams");
        request.setSecurityManager("Sarah Johnson");
        request.setAuthorizingOfficial("David Martinez");
        request.setConditions(Arrays.asList(condition1, condition2));

        assertNotNull(request);
        assertTrue(request.getName().contains("ERP"));
        assertTrue(request.getSspItemId().contains("erp"));
        assertTrue(request.getSarItemId().contains("erp"));
        assertEquals(5L, request.getTemplateId());
        assertEquals(4, request.getVariableValues().size());
        assertTrue(request.getVariableValues().get("systemName").contains("Enterprise"));
        assertEquals("2025-02-01", request.getDateAuthorized());
        assertEquals("2028-01-31", request.getDateExpired());
        assertNotNull(request.getSystemOwner());
        assertNotNull(request.getSecurityManager());
        assertNotNull(request.getAuthorizingOfficial());
        assertEquals(2, request.getConditions().size());
        assertTrue(request.getConditions().get(0).getCondition().contains("annual"));
    }

    @Test
    void testWithSpecialCharactersInName() {
        AuthorizationRequest request = new AuthorizationRequest();
        String nameWithSpecialChars = "ATO v2.0 (Production) [2025-Q1] - High Impact";
        request.setName(nameWithSpecialChars);
        assertEquals(nameWithSpecialChars, request.getName());
        assertTrue(request.getName().contains("(Production)"));
    }

    @Test
    void testPartialRequest() {
        Map<String, String> variables = new HashMap<>();
        variables.put("systemName", "Test System");

        AuthorizationRequest request = new AuthorizationRequest(
                "Test ATO",
                "ssp-test",
                1L,
                variables
        );

        assertEquals("Test ATO", request.getName());
        assertNull(request.getDateAuthorized());
        assertNull(request.getSystemOwner());
        assertEquals(0, request.getConditions().size());
    }

    @Test
    void testWithEmptyStrings() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setName("");
        request.setSspItemId("");
        request.setDateAuthorized("");
        request.setSystemOwner("");

        assertEquals("", request.getName());
        assertEquals("", request.getSspItemId());
        assertEquals("", request.getDateAuthorized());
        assertEquals("", request.getSystemOwner());
    }
}
