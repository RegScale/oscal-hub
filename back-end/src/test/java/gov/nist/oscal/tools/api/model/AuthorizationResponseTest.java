package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.ConditionOfApproval;
import gov.nist.oscal.tools.api.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationResponseTest {

    @Test
    void testNoArgsConstructor() {
        AuthorizationResponse response = new AuthorizationResponse();

        assertNotNull(response);
        assertNull(response.getId());
        assertNull(response.getName());
        assertNull(response.getSspItemId());
        assertNull(response.getSarItemId());
        assertNull(response.getTemplateId());
        assertNull(response.getTemplateName());
        assertNull(response.getVariableValues());
        assertNull(response.getCompletedContent());
        assertNull(response.getAuthorizedBy());
        assertNull(response.getAuthorizedAt());
        assertNull(response.getCreatedAt());
        assertNull(response.getDateAuthorized());
        assertNull(response.getDateExpired());
        assertNull(response.getSystemOwner());
        assertNull(response.getSecurityManager());
        assertNull(response.getAuthorizingOfficial());
        assertNull(response.getConditions());
        assertNull(response.getDigitalSignatureMethod());
        assertNull(response.getSignerCertificate());
        assertNull(response.getSignerCommonName());
        assertNull(response.getSignerEmail());
        assertNull(response.getSignerEdipi());
        assertNull(response.getCertificateIssuer());
        assertNull(response.getCertificateSerial());
        assertNull(response.getCertificateNotBefore());
        assertNull(response.getCertificateNotAfter());
        assertNull(response.getSignatureTimestamp());
        assertNull(response.getDocumentHash());
        assertNull(response.getCertificateVerified());
        assertNull(response.getCertificateVerificationDate());
        assertNull(response.getCertificateVerificationNotes());
    }

    @Test
    void testEntityConstructorWithCompleteAuthorization() {
        // Create mock user
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("john.doe");

        // Create mock template
        AuthorizationTemplate template = mock(AuthorizationTemplate.class);
        when(template.getId()).thenReturn(10L);
        when(template.getName()).thenReturn("ATO Template");

        // Create mock conditions
        ConditionOfApproval condition1 = mock(ConditionOfApproval.class);
        Authorization mockAuth = mock(Authorization.class);
        when(mockAuth.getId()).thenReturn(1L);
        when(condition1.getId()).thenReturn(100L);
        when(condition1.getAuthorization()).thenReturn(mockAuth);
        when(condition1.getCondition()).thenReturn("Complete security assessment");
        when(condition1.getConditionType()).thenReturn(ConditionOfApproval.ConditionType.MANDATORY);
        when(condition1.getDueDate()).thenReturn(LocalDate.of(2026, 6, 30));
        when(condition1.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(condition1.getUpdatedAt()).thenReturn(LocalDateTime.now());

        List<ConditionOfApproval> conditions = new ArrayList<>();
        conditions.add(condition1);

        // Create variable values
        Map<String, String> variableValues = new HashMap<>();
        variableValues.put("systemName", "Test System");
        variableValues.put("authorizationDate", "2025-01-01");

        // Create mock authorization
        Authorization authorization = mock(Authorization.class);
        when(authorization.getId()).thenReturn(1L);
        when(authorization.getName()).thenReturn("Production System ATO");
        when(authorization.getSspItemId()).thenReturn("ssp-12345");
        when(authorization.getSarItemId()).thenReturn("sar-67890");
        when(authorization.getTemplate()).thenReturn(template);
        when(authorization.getVariableValues()).thenReturn(variableValues);
        when(authorization.getCompletedContent()).thenReturn("Authorization to Operate for Test System granted on 2025-01-01");
        when(authorization.getAuthorizedBy()).thenReturn(user);
        when(authorization.getAuthorizedAt()).thenReturn(LocalDateTime.of(2025, 1, 1, 10, 0));
        when(authorization.getCreatedAt()).thenReturn(LocalDateTime.of(2024, 12, 1, 9, 0));
        when(authorization.getDateAuthorized()).thenReturn(LocalDate.of(2025, 1, 1));
        when(authorization.getDateExpired()).thenReturn(LocalDate.of(2028, 1, 1));
        when(authorization.getSystemOwner()).thenReturn("Alice Smith");
        when(authorization.getSecurityManager()).thenReturn("Bob Jones");
        when(authorization.getAuthorizingOfficial()).thenReturn("Carol Williams");
        when(authorization.getConditions()).thenReturn(conditions);

        // Digital signature fields
        when(authorization.getDigitalSignatureMethod()).thenReturn("TLS_CLIENT_CERT");
        when(authorization.getSignerCertificate()).thenReturn("MIIDXTCCAkWgAwIBAgIBADA...");
        when(authorization.getSignerCommonName()).thenReturn("John Doe 1234567890");
        when(authorization.getSignerEmail()).thenReturn("john.doe@example.gov");
        when(authorization.getSignerEdipi()).thenReturn("1234567890");
        when(authorization.getCertificateIssuer()).thenReturn("CN=DoD CA-1, OU=PKI, OU=DoD, O=U.S. Government, C=US");
        when(authorization.getCertificateSerial()).thenReturn("0A1B2C3D");
        when(authorization.getCertificateNotBefore()).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
        when(authorization.getCertificateNotAfter()).thenReturn(LocalDateTime.of(2027, 1, 1, 0, 0));
        when(authorization.getSignatureTimestamp()).thenReturn(LocalDateTime.of(2025, 1, 1, 10, 0));
        when(authorization.getDocumentHash()).thenReturn("a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890");
        when(authorization.getCertificateVerified()).thenReturn(true);
        when(authorization.getCertificateVerificationDate()).thenReturn(LocalDateTime.of(2025, 1, 1, 9, 55));
        when(authorization.getCertificateVerificationNotes()).thenReturn("Certificate validated successfully");

        AuthorizationResponse response = new AuthorizationResponse(authorization);

        // Verify all fields
        assertEquals(1L, response.getId());
        assertEquals("Production System ATO", response.getName());
        assertEquals("ssp-12345", response.getSspItemId());
        assertEquals("sar-67890", response.getSarItemId());
        assertEquals(10L, response.getTemplateId());
        assertEquals("ATO Template", response.getTemplateName());
        assertEquals(2, response.getVariableValues().size());
        assertEquals("Test System", response.getVariableValues().get("systemName"));
        assertTrue(response.getCompletedContent().contains("Test System"));
        assertEquals("john.doe", response.getAuthorizedBy());
        assertEquals(LocalDateTime.of(2025, 1, 1, 10, 0), response.getAuthorizedAt());
        assertEquals(LocalDateTime.of(2024, 12, 1, 9, 0), response.getCreatedAt());
        assertEquals(LocalDate.of(2025, 1, 1), response.getDateAuthorized());
        assertEquals(LocalDate.of(2028, 1, 1), response.getDateExpired());
        assertEquals("Alice Smith", response.getSystemOwner());
        assertEquals("Bob Jones", response.getSecurityManager());
        assertEquals("Carol Williams", response.getAuthorizingOfficial());
        assertEquals(1, response.getConditions().size());

        // Verify digital signature fields
        assertEquals("TLS_CLIENT_CERT", response.getDigitalSignatureMethod());
        assertTrue(response.getSignerCertificate().startsWith("MII"));
        assertEquals("John Doe 1234567890", response.getSignerCommonName());
        assertEquals("john.doe@example.gov", response.getSignerEmail());
        assertEquals("1234567890", response.getSignerEdipi());
        assertTrue(response.getCertificateIssuer().contains("DoD"));
        assertEquals("0A1B2C3D", response.getCertificateSerial());
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), response.getCertificateNotBefore());
        assertEquals(LocalDateTime.of(2027, 1, 1, 0, 0), response.getCertificateNotAfter());
        assertEquals(LocalDateTime.of(2025, 1, 1, 10, 0), response.getSignatureTimestamp());
        assertEquals(64, response.getDocumentHash().length());
        assertTrue(response.getCertificateVerified());
        assertEquals(LocalDateTime.of(2025, 1, 1, 9, 55), response.getCertificateVerificationDate());
        assertEquals("Certificate validated successfully", response.getCertificateVerificationNotes());
    }

    @Test
    void testEntityConstructorWithMinimalFields() {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("user");

        AuthorizationTemplate template = mock(AuthorizationTemplate.class);
        when(template.getId()).thenReturn(1L);
        when(template.getName()).thenReturn("Template");

        Authorization authorization = mock(Authorization.class);
        when(authorization.getId()).thenReturn(1L);
        when(authorization.getName()).thenReturn("Auth");
        when(authorization.getSspItemId()).thenReturn("ssp-1");
        when(authorization.getTemplate()).thenReturn(template);
        when(authorization.getVariableValues()).thenReturn(new HashMap<>());
        when(authorization.getCompletedContent()).thenReturn("Content");
        when(authorization.getAuthorizedBy()).thenReturn(user);
        when(authorization.getAuthorizedAt()).thenReturn(LocalDateTime.now());
        when(authorization.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(authorization.getConditions()).thenReturn(new ArrayList<>());

        AuthorizationResponse response = new AuthorizationResponse(authorization);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("user", response.getAuthorizedBy());
        assertEquals(0, response.getConditions().size());
    }

    @Test
    void testSetId() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setId(100L);
        assertEquals(100L, response.getId());
    }

    @Test
    void testSetName() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setName("System Authorization");
        assertEquals("System Authorization", response.getName());
    }

    @Test
    void testSetSspItemId() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setSspItemId("ssp-99999");
        assertEquals("ssp-99999", response.getSspItemId());
    }

    @Test
    void testSetSarItemId() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setSarItemId("sar-88888");
        assertEquals("sar-88888", response.getSarItemId());
    }

    @Test
    void testSetTemplateId() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setTemplateId(50L);
        assertEquals(50L, response.getTemplateId());
    }

    @Test
    void testSetTemplateName() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setTemplateName("Custom Template");
        assertEquals("Custom Template", response.getTemplateName());
    }

    @Test
    void testSetVariableValues() {
        AuthorizationResponse response = new AuthorizationResponse();
        Map<String, String> vars = new HashMap<>();
        vars.put("key1", "value1");
        vars.put("key2", "value2");
        response.setVariableValues(vars);
        assertEquals(2, response.getVariableValues().size());
        assertEquals("value1", response.getVariableValues().get("key1"));
    }

    @Test
    void testSetCompletedContent() {
        AuthorizationResponse response = new AuthorizationResponse();
        String content = "This system is authorized to operate";
        response.setCompletedContent(content);
        assertEquals(content, response.getCompletedContent());
    }

    @Test
    void testSetAuthorizedBy() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setAuthorizedBy("admin.user");
        assertEquals("admin.user", response.getAuthorizedBy());
    }

    @Test
    void testSetAuthorizedAt() {
        AuthorizationResponse response = new AuthorizationResponse();
        LocalDateTime timestamp = LocalDateTime.of(2025, 5, 15, 14, 30);
        response.setAuthorizedAt(timestamp);
        assertEquals(timestamp, response.getAuthorizedAt());
    }

    @Test
    void testSetCreatedAt() {
        AuthorizationResponse response = new AuthorizationResponse();
        LocalDateTime timestamp = LocalDateTime.of(2025, 4, 10, 9, 0);
        response.setCreatedAt(timestamp);
        assertEquals(timestamp, response.getCreatedAt());
    }

    @Test
    void testSetDateAuthorized() {
        AuthorizationResponse response = new AuthorizationResponse();
        LocalDate date = LocalDate.of(2025, 3, 1);
        response.setDateAuthorized(date);
        assertEquals(date, response.getDateAuthorized());
    }

    @Test
    void testSetDateExpired() {
        AuthorizationResponse response = new AuthorizationResponse();
        LocalDate date = LocalDate.of(2028, 3, 1);
        response.setDateExpired(date);
        assertEquals(date, response.getDateExpired());
    }

    @Test
    void testSetSystemOwner() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setSystemOwner("Owner Name");
        assertEquals("Owner Name", response.getSystemOwner());
    }

    @Test
    void testSetSecurityManager() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setSecurityManager("Security Manager Name");
        assertEquals("Security Manager Name", response.getSecurityManager());
    }

    @Test
    void testSetAuthorizingOfficial() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setAuthorizingOfficial("Official Name");
        assertEquals("Official Name", response.getAuthorizingOfficial());
    }

    @Test
    void testSetConditions() {
        AuthorizationResponse response = new AuthorizationResponse();
        List<ConditionOfApprovalResponse> conditions = new ArrayList<>();
        conditions.add(new ConditionOfApprovalResponse());
        response.setConditions(conditions);
        assertEquals(1, response.getConditions().size());
    }

    @Test
    void testSetDigitalSignatureMethod() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setDigitalSignatureMethod("TLS_CLIENT_CERT");
        assertEquals("TLS_CLIENT_CERT", response.getDigitalSignatureMethod());
    }

    @Test
    void testSetSignerCertificate() {
        AuthorizationResponse response = new AuthorizationResponse();
        String cert = "MIIDXTCCAkWgAwIBAgIBADA...";
        response.setSignerCertificate(cert);
        assertEquals(cert, response.getSignerCertificate());
    }

    @Test
    void testSetSignerCommonName() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setSignerCommonName("Jane Doe 0987654321");
        assertEquals("Jane Doe 0987654321", response.getSignerCommonName());
    }

    @Test
    void testSetSignerEmail() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setSignerEmail("jane.doe@example.gov");
        assertEquals("jane.doe@example.gov", response.getSignerEmail());
    }

    @Test
    void testSetSignerEdipi() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setSignerEdipi("0987654321");
        assertEquals("0987654321", response.getSignerEdipi());
    }

    @Test
    void testSetCertificateIssuer() {
        AuthorizationResponse response = new AuthorizationResponse();
        String issuer = "CN=DoD CA-2, OU=PKI, OU=DoD, O=U.S. Government, C=US";
        response.setCertificateIssuer(issuer);
        assertEquals(issuer, response.getCertificateIssuer());
    }

    @Test
    void testSetCertificateSerial() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setCertificateSerial("1A2B3C4D");
        assertEquals("1A2B3C4D", response.getCertificateSerial());
    }

    @Test
    void testSetCertificateNotBefore() {
        AuthorizationResponse response = new AuthorizationResponse();
        LocalDateTime timestamp = LocalDateTime.of(2023, 1, 1, 0, 0);
        response.setCertificateNotBefore(timestamp);
        assertEquals(timestamp, response.getCertificateNotBefore());
    }

    @Test
    void testSetCertificateNotAfter() {
        AuthorizationResponse response = new AuthorizationResponse();
        LocalDateTime timestamp = LocalDateTime.of(2026, 12, 31, 23, 59);
        response.setCertificateNotAfter(timestamp);
        assertEquals(timestamp, response.getCertificateNotAfter());
    }

    @Test
    void testSetSignatureTimestamp() {
        AuthorizationResponse response = new AuthorizationResponse();
        LocalDateTime timestamp = LocalDateTime.of(2025, 6, 15, 10, 30);
        response.setSignatureTimestamp(timestamp);
        assertEquals(timestamp, response.getSignatureTimestamp());
    }

    @Test
    void testSetDocumentHash() {
        AuthorizationResponse response = new AuthorizationResponse();
        String hash = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        response.setDocumentHash(hash);
        assertEquals(hash, response.getDocumentHash());
    }

    @Test
    void testSetCertificateVerified() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setCertificateVerified(true);
        assertTrue(response.getCertificateVerified());

        response.setCertificateVerified(false);
        assertFalse(response.getCertificateVerified());
    }

    @Test
    void testSetCertificateVerificationDate() {
        AuthorizationResponse response = new AuthorizationResponse();
        LocalDateTime timestamp = LocalDateTime.of(2025, 7, 20, 11, 45);
        response.setCertificateVerificationDate(timestamp);
        assertEquals(timestamp, response.getCertificateVerificationDate());
    }

    @Test
    void testSetCertificateVerificationNotes() {
        AuthorizationResponse response = new AuthorizationResponse();
        String notes = "Certificate chain validated against DoD PKI root";
        response.setCertificateVerificationNotes(notes);
        assertEquals(notes, response.getCertificateVerificationNotes());
    }

    @Test
    void testWithEmptyVariableValues() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setVariableValues(new HashMap<>());
        assertNotNull(response.getVariableValues());
        assertTrue(response.getVariableValues().isEmpty());
    }

    @Test
    void testWithEmptyConditions() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setConditions(new ArrayList<>());
        assertNotNull(response.getConditions());
        assertTrue(response.getConditions().isEmpty());
    }

    @Test
    void testSetAllFieldsToNull() {
        AuthorizationResponse response = new AuthorizationResponse();
        // Set all fields to non-null first
        response.setId(1L);
        response.setName("Name");
        response.setSspItemId("ssp");
        response.setSarItemId("sar");
        response.setTemplateId(1L);
        response.setTemplateName("Template");
        response.setVariableValues(new HashMap<>());
        response.setCompletedContent("Content");
        response.setAuthorizedBy("user");
        response.setAuthorizedAt(LocalDateTime.now());
        response.setCreatedAt(LocalDateTime.now());
        response.setDateAuthorized(LocalDate.now());
        response.setDateExpired(LocalDate.now());
        response.setSystemOwner("Owner");
        response.setSecurityManager("Manager");
        response.setAuthorizingOfficial("Official");
        response.setConditions(new ArrayList<>());
        response.setDigitalSignatureMethod("Method");
        response.setSignerCertificate("Cert");
        response.setSignerCommonName("CN");
        response.setSignerEmail("email");
        response.setSignerEdipi("edipi");
        response.setCertificateIssuer("Issuer");
        response.setCertificateSerial("Serial");
        response.setCertificateNotBefore(LocalDateTime.now());
        response.setCertificateNotAfter(LocalDateTime.now());
        response.setSignatureTimestamp(LocalDateTime.now());
        response.setDocumentHash("hash");
        response.setCertificateVerified(true);
        response.setCertificateVerificationDate(LocalDateTime.now());
        response.setCertificateVerificationNotes("Notes");

        // Set all to null
        response.setId(null);
        response.setName(null);
        response.setSspItemId(null);
        response.setSarItemId(null);
        response.setTemplateId(null);
        response.setTemplateName(null);
        response.setVariableValues(null);
        response.setCompletedContent(null);
        response.setAuthorizedBy(null);
        response.setAuthorizedAt(null);
        response.setCreatedAt(null);
        response.setDateAuthorized(null);
        response.setDateExpired(null);
        response.setSystemOwner(null);
        response.setSecurityManager(null);
        response.setAuthorizingOfficial(null);
        response.setConditions(null);
        response.setDigitalSignatureMethod(null);
        response.setSignerCertificate(null);
        response.setSignerCommonName(null);
        response.setSignerEmail(null);
        response.setSignerEdipi(null);
        response.setCertificateIssuer(null);
        response.setCertificateSerial(null);
        response.setCertificateNotBefore(null);
        response.setCertificateNotAfter(null);
        response.setSignatureTimestamp(null);
        response.setDocumentHash(null);
        response.setCertificateVerified(null);
        response.setCertificateVerificationDate(null);
        response.setCertificateVerificationNotes(null);

        // Verify all null
        assertNull(response.getId());
        assertNull(response.getName());
        assertNull(response.getSspItemId());
        assertNull(response.getSarItemId());
        assertNull(response.getTemplateId());
        assertNull(response.getTemplateName());
        assertNull(response.getVariableValues());
        assertNull(response.getCompletedContent());
        assertNull(response.getAuthorizedBy());
        assertNull(response.getAuthorizedAt());
        assertNull(response.getCreatedAt());
        assertNull(response.getDateAuthorized());
        assertNull(response.getDateExpired());
        assertNull(response.getSystemOwner());
        assertNull(response.getSecurityManager());
        assertNull(response.getAuthorizingOfficial());
        assertNull(response.getConditions());
        assertNull(response.getDigitalSignatureMethod());
        assertNull(response.getSignerCertificate());
        assertNull(response.getSignerCommonName());
        assertNull(response.getSignerEmail());
        assertNull(response.getSignerEdipi());
        assertNull(response.getCertificateIssuer());
        assertNull(response.getCertificateSerial());
        assertNull(response.getCertificateNotBefore());
        assertNull(response.getCertificateNotAfter());
        assertNull(response.getSignatureTimestamp());
        assertNull(response.getDocumentHash());
        assertNull(response.getCertificateVerified());
        assertNull(response.getCertificateVerificationDate());
        assertNull(response.getCertificateVerificationNotes());
    }
}
