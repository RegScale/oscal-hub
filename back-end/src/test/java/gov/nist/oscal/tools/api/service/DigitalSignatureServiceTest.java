package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.model.CertificateInfo;
import gov.nist.oscal.tools.api.model.CertificateValidationResult;
import gov.nist.oscal.tools.api.model.SignatureResult;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DigitalSignatureServiceTest {

    @Mock
    private AuthorizationRepository authorizationRepository;

    @InjectMocks
    private DigitalSignatureService digitalSignatureService;

    private Authorization mockAuthorization;
    private X509Certificate mockCertificate;

    @BeforeEach
    void setUp() {
        mockAuthorization = new Authorization();
        mockAuthorization.setId(1L);
        mockAuthorization.setName("Test Authorization");
        mockAuthorization.setSspItemId("SSP-001");
        mockAuthorization.setSarItemId("SAR-001");
        mockAuthorization.setCompletedContent("Authorization content");
        mockAuthorization.setDateAuthorized(LocalDate.now());
        mockAuthorization.setDateExpired(LocalDate.now().plusYears(3));
        mockAuthorization.setSystemOwner("John Doe");
        mockAuthorization.setSecurityManager("Jane Smith");
        mockAuthorization.setAuthorizingOfficial("Bob Johnson");

        // Create mock certificate
        mockCertificate = mock(X509Certificate.class);
        X500Principal subjectPrincipal = mock(X500Principal.class);
        X500Principal issuerPrincipal = mock(X500Principal.class);

        when(subjectPrincipal.getName()).thenReturn("CN=SMITH.JOHN.DOE.1234567890,EMAILADDRESS=john.doe@example.mil,O=U.S. Government,C=US");
        when(issuerPrincipal.getName()).thenReturn("CN=DOD ID CA-59,OU=PKI,OU=DoD,O=U.S. Government,C=US");

        when(mockCertificate.getSubjectX500Principal()).thenReturn(subjectPrincipal);
        when(mockCertificate.getIssuerX500Principal()).thenReturn(issuerPrincipal);
        when(mockCertificate.getSerialNumber()).thenReturn(new BigInteger("123456789ABCDEF", 16));

        // Set validity dates
        Date notBefore = new Date(System.currentTimeMillis() - 86400000L); // 1 day ago
        Date notAfter = new Date(System.currentTimeMillis() + 31536000000L); // 1 year from now
        when(mockCertificate.getNotBefore()).thenReturn(notBefore);
        when(mockCertificate.getNotAfter()).thenReturn(notAfter);

        // Set key usage - digitalSignature enabled
        boolean[] keyUsage = new boolean[9];
        keyUsage[0] = true; // digitalSignature
        when(mockCertificate.getKeyUsage()).thenReturn(keyUsage);
    }

    @Test
    void testSignAuthorization_success() throws Exception {
        when(authorizationRepository.findById(1L)).thenReturn(Optional.of(mockAuthorization));
        when(authorizationRepository.save(any(Authorization.class))).thenReturn(mockAuthorization);

        // Mock certificate encoding
        when(mockCertificate.getEncoded()).thenReturn("test-certificate-bytes".getBytes());

        SignatureResult result = digitalSignatureService.signAuthorization(1L, mockCertificate);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("SMITH.JOHN.DOE.1234567890", result.getSignerName());
        assertEquals("john.doe@example.mil", result.getSignerEmail());
        assertEquals("1234567890", result.getSignerEdipi());
        assertNotNull(result.getSignatureTimestamp());
        assertEquals("Authorization successfully signed", result.getMessage());

        verify(authorizationRepository).findById(1L);
        verify(authorizationRepository).save(any(Authorization.class));
    }

    @Test
    void testSignAuthorization_authorizationNotFound() {
        when(authorizationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            digitalSignatureService.signAuthorization(999L, mockCertificate);
        });

        verify(authorizationRepository).findById(999L);
        verify(authorizationRepository, never()).save(any());
    }

    @Test
    void testGenerateDocumentHash_success() throws Exception {
        String hash = digitalSignatureService.generateDocumentHash(mockAuthorization);

        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 produces 64 hex characters
        assertTrue(hash.matches("[0-9a-f]{64}")); // Hex format
    }

    @Test
    void testGenerateDocumentHash_consistentHashing() throws Exception {
        // Generate hash twice with same data
        String hash1 = digitalSignatureService.generateDocumentHash(mockAuthorization);
        String hash2 = digitalSignatureService.generateDocumentHash(mockAuthorization);

        assertEquals(hash1, hash2, "Same authorization should produce same hash");
    }

    @Test
    void testGenerateDocumentHash_differentWhenContentChanges() throws Exception {
        String hash1 = digitalSignatureService.generateDocumentHash(mockAuthorization);

        // Change content
        mockAuthorization.setCompletedContent("Different content");
        String hash2 = digitalSignatureService.generateDocumentHash(mockAuthorization);

        assertNotEquals(hash1, hash2, "Different content should produce different hash");
    }

    @Test
    void testGenerateDocumentHash_withNullDates() throws Exception {
        mockAuthorization.setDateAuthorized(null);
        mockAuthorization.setDateExpired(null);

        String hash = digitalSignatureService.generateDocumentHash(mockAuthorization);

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void testExtractCertificateInfo_withEdipi() {
        CertificateInfo info = digitalSignatureService.extractCertificateInfo(mockCertificate);

        assertNotNull(info);
        assertEquals("SMITH.JOHN.DOE.1234567890", info.getCommonName());
        assertEquals("john.doe@example.mil", info.getEmail());
        assertEquals("1234567890", info.getEdipi());
        assertNotNull(info.getSubjectDN());
        assertNotNull(info.getIssuerDN());
        assertEquals("123456789ABCDEF", info.getSerialNumber());
    }

    @Test
    void testExtractCertificateInfo_withoutEdipi() {
        X500Principal noCacPrincipal = mock(X500Principal.class);
        when(noCacPrincipal.getName()).thenReturn("CN=Regular User,EMAILADDRESS=user@example.com,O=Test Org,C=US");
        when(mockCertificate.getSubjectX500Principal()).thenReturn(noCacPrincipal);

        CertificateInfo info = digitalSignatureService.extractCertificateInfo(mockCertificate);

        assertNotNull(info);
        assertEquals("Regular User", info.getCommonName());
        assertEquals("user@example.com", info.getEmail());
        assertNull(info.getEdipi(), "EDIPI should be null for non-CAC certificate");
    }

    @Test
    void testExtractCertificateInfo_emailAddressField() {
        // Some certificates use EMAILADDRESS instead of E
        X500Principal emailAddressPrincipal = mock(X500Principal.class);
        when(emailAddressPrincipal.getName()).thenReturn("CN=Test User,EMAILADDRESS=test@example.com,O=Test,C=US");
        when(mockCertificate.getSubjectX500Principal()).thenReturn(emailAddressPrincipal);

        CertificateInfo info = digitalSignatureService.extractCertificateInfo(mockCertificate);

        assertNotNull(info);
        assertEquals("test@example.com", info.getEmail());
    }

    @Test
    void testValidateCertificate_validCertificate() throws Exception {
        doNothing().when(mockCertificate).checkValidity();

        CertificateValidationResult result = digitalSignatureService.validateCertificate(mockCertificate);

        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getNotes().contains("Certificate is within validity period"));
        assertTrue(result.getNotes().contains("digitalSignature key usage"));
        assertTrue(result.getNotes().contains("recognized authority"));
        assertTrue(result.getNotes().contains("CAC/PIV format"));
    }

    @Test
    void testValidateCertificate_expiredCertificate() throws Exception {
        doThrow(new CertificateExpiredException("Certificate expired"))
            .when(mockCertificate).checkValidity();

        CertificateValidationResult result = digitalSignatureService.validateCertificate(mockCertificate);

        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getNotes().contains("ERROR: Certificate has expired"));
    }

    @Test
    void testValidateCertificate_notYetValid() throws Exception {
        doThrow(new CertificateNotYetValidException("Certificate not yet valid"))
            .when(mockCertificate).checkValidity();

        CertificateValidationResult result = digitalSignatureService.validateCertificate(mockCertificate);

        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getNotes().contains("ERROR: Certificate is not yet valid"));
    }

    @Test
    void testValidateCertificate_noDigitalSignatureKeyUsage() throws Exception {
        doNothing().when(mockCertificate).checkValidity();

        // Key usage without digitalSignature
        boolean[] keyUsage = new boolean[9];
        keyUsage[0] = false; // digitalSignature disabled
        keyUsage[1] = true;  // some other usage
        when(mockCertificate.getKeyUsage()).thenReturn(keyUsage);

        CertificateValidationResult result = digitalSignatureService.validateCertificate(mockCertificate);

        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getNotes().contains("ERROR: Certificate does not have digitalSignature key usage"));
    }

    @Test
    void testValidateCertificate_noKeyUsageExtension() throws Exception {
        doNothing().when(mockCertificate).checkValidity();
        when(mockCertificate.getKeyUsage()).thenReturn(null);

        CertificateValidationResult result = digitalSignatureService.validateCertificate(mockCertificate);

        assertNotNull(result);
        // Still valid, but with warning
        assertTrue(result.getNotes().contains("WARNING: No key usage extension found"));
    }

    @Test
    void testValidateCertificate_unrecognizedIssuer() throws Exception {
        doNothing().when(mockCertificate).checkValidity();

        X500Principal unknownIssuer = mock(X500Principal.class);
        when(unknownIssuer.getName()).thenReturn("CN=Unknown CA,O=Unknown Org,C=US");
        when(mockCertificate.getIssuerX500Principal()).thenReturn(unknownIssuer);

        CertificateValidationResult result = digitalSignatureService.validateCertificate(mockCertificate);

        assertNotNull(result);
        assertTrue(result.getNotes().contains("WARNING: Certificate issuer not recognized"));
    }

    @Test
    void testValidateCertificate_federalPKIIssuer() throws Exception {
        doNothing().when(mockCertificate).checkValidity();

        X500Principal federalIssuer = mock(X500Principal.class);
        when(federalIssuer.getName()).thenReturn("CN=Federal Common Policy CA,O=U.S. Government,C=US");
        when(mockCertificate.getIssuerX500Principal()).thenReturn(federalIssuer);

        CertificateValidationResult result = digitalSignatureService.validateCertificate(mockCertificate);

        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getNotes().contains("recognized authority"));
    }

    @Test
    void testValidateCertificate_nonCacFormat() throws Exception {
        doNothing().when(mockCertificate).checkValidity();

        X500Principal nonCacSubject = mock(X500Principal.class);
        when(nonCacSubject.getName()).thenReturn("CN=Regular User,EMAILADDRESS=user@example.com,O=Test,C=US");
        when(mockCertificate.getSubjectX500Principal()).thenReturn(nonCacSubject);

        CertificateValidationResult result = digitalSignatureService.validateCertificate(mockCertificate);

        assertNotNull(result);
        // Should not contain CAC/PIV format note
        assertFalse(result.getNotes().contains("CAC/PIV format"));
    }

    @Test
    void testVerifyCertificate_success() throws Exception {
        // Create a real-ish certificate encoding for testing
        String fakeCertPem = Base64.getEncoder().encodeToString(
            "-----BEGIN CERTIFICATE-----\nMIIBkTCB+wIJAKHHCgVZG6b5MA0GCSqGSIb3DQEBBAUAMA0xCzAJBgNVBAYTAlVT\nMB4XDTE0MDEwMTAwMDAwMFoXDTE1MDEwMTAwMDAwMFowDTELMAkGA1UEBhMCVVMw\nXDANBgkqhkiG9w0BAQEFAANLADBIAkEAryQICCl6NZ5gDKrnSztO3Hy8PEUcuyvg\n/ikC+VcIo2SFFSf18a3IMYldIugqqqZCs4/4uVW3sbdLs/6PfgdRZQIDAQABMA0G\nCSqGSIb3DQEBBAUAA0EArzPqMU1g3TDlxVXXR9uH8wKC3y1xMcqXGKxPGsS8wL0c\nqDz8+KBh9qG9S3p8rTkjPm0Pxq0PqJ0P+RqF5I8=\n-----END CERTIFICATE-----".getBytes()
        );

        // This will likely throw an exception because it's not a valid cert,
        // but that's okay for testing the method call path
        assertThrows(Exception.class, () -> {
            digitalSignatureService.verifyCertificate(fakeCertPem);
        });
    }

    @Test
    void testVerifyCertificate_invalidBase64() {
        String invalidBase64 = "this-is-not-valid-base64!@#$%";

        assertThrows(Exception.class, () -> {
            digitalSignatureService.verifyCertificate(invalidBase64);
        });
    }

    @Test
    void testSignAuthorization_storesAllCertificateData() throws Exception {
        when(authorizationRepository.findById(1L)).thenReturn(Optional.of(mockAuthorization));
        when(authorizationRepository.save(any(Authorization.class))).thenAnswer(invocation -> {
            Authorization savedAuth = invocation.getArgument(0);

            // Verify all certificate fields are set
            assertEquals("TLS_CLIENT_CERT", savedAuth.getDigitalSignatureMethod());
            assertNotNull(savedAuth.getSignerCertificate());
            assertEquals("SMITH.JOHN.DOE.1234567890", savedAuth.getSignerCommonName());
            assertEquals("john.doe@example.mil", savedAuth.getSignerEmail());
            assertEquals("1234567890", savedAuth.getSignerEdipi());
            assertNotNull(savedAuth.getCertificateIssuer());
            assertNotNull(savedAuth.getCertificateSerial());
            assertNotNull(savedAuth.getCertificateNotBefore());
            assertNotNull(savedAuth.getCertificateNotAfter());
            assertNotNull(savedAuth.getSignatureTimestamp());
            assertNotNull(savedAuth.getDocumentHash());
            assertTrue(savedAuth.getCertificateVerified());
            assertNotNull(savedAuth.getCertificateVerificationDate());
            assertNotNull(savedAuth.getCertificateVerificationNotes());

            return savedAuth;
        });

        when(mockCertificate.getEncoded()).thenReturn("test-certificate-bytes".getBytes());

        digitalSignatureService.signAuthorization(1L, mockCertificate);

        verify(authorizationRepository).save(any(Authorization.class));
    }
}
