package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SignatureDetailsResponseTest {

    @Test
    void testNoArgsConstructor() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();

        assertNotNull(response);
        assertFalse(response.isSigned()); // primitive boolean defaults to false
        assertNull(response.getSignerName());
        assertNull(response.getSignerEmail());
        assertNull(response.getSignerEdipi());
        assertNull(response.getSignatureTimestamp());
        assertNull(response.getCertificateIssuer());
        assertNull(response.getCertificateSerial());
        assertNull(response.getCertificateNotBefore());
        assertNull(response.getCertificateNotAfter());
        assertNull(response.getCertificateVerified());
        assertNull(response.getVerificationDate());
        assertNull(response.getVerificationNotes());
        assertNull(response.getMessage());
    }

    @Test
    void testMessageConstructor() {
        String message = "Authorization is not signed";

        SignatureDetailsResponse response = new SignatureDetailsResponse(message);

        assertNotNull(response);
        assertFalse(response.isSigned()); // Constructor sets signed to false
        assertEquals(message, response.getMessage());
        assertNull(response.getSignerName());
    }

    @Test
    void testMessageConstructorWithNull() {
        SignatureDetailsResponse response = new SignatureDetailsResponse(null);

        assertNotNull(response);
        assertFalse(response.isSigned());
        assertNull(response.getMessage());
    }

    @Test
    void testBuilderWithAllFields() {
        LocalDateTime signatureTime = LocalDateTime.now();
        LocalDateTime notBefore = signatureTime.minusYears(1);
        LocalDateTime notAfter = signatureTime.plusYears(2);
        LocalDateTime verificationTime = LocalDateTime.now();

        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                .signed(true)
                .signerName("John Doe")
                .signerEmail("john.doe@example.com")
                .signerEdipi("1234567890")
                .signatureTimestamp(signatureTime)
                .certificateIssuer("CN=Example CA,O=Example Corp")
                .certificateSerial("1A:2B:3C:4D:5E:6F")
                .certificateNotBefore(notBefore)
                .certificateNotAfter(notAfter)
                .certificateVerified(true)
                .verificationDate(verificationTime)
                .verificationNotes("Certificate verified successfully")
                .message("Signature is valid")
                .build();

        assertNotNull(response);
        assertTrue(response.isSigned());
        assertEquals("John Doe", response.getSignerName());
        assertEquals("john.doe@example.com", response.getSignerEmail());
        assertEquals("1234567890", response.getSignerEdipi());
        assertEquals(signatureTime, response.getSignatureTimestamp());
        assertEquals("CN=Example CA,O=Example Corp", response.getCertificateIssuer());
        assertEquals("1A:2B:3C:4D:5E:6F", response.getCertificateSerial());
        assertEquals(notBefore, response.getCertificateNotBefore());
        assertEquals(notAfter, response.getCertificateNotAfter());
        assertTrue(response.getCertificateVerified());
        assertEquals(verificationTime, response.getVerificationDate());
        assertEquals("Certificate verified successfully", response.getVerificationNotes());
        assertEquals("Signature is valid", response.getMessage());
    }

    @Test
    void testBuilderWithMinimalFields() {
        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                .signed(false)
                .message("Not signed")
                .build();

        assertNotNull(response);
        assertFalse(response.isSigned());
        assertEquals("Not signed", response.getMessage());
        assertNull(response.getSignerName());
        assertNull(response.getCertificateVerified());
    }

    @Test
    void testBuilderWithNullValues() {
        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                .signerName(null)
                .signerEmail(null)
                .certificateVerified(null)
                .build();

        assertNotNull(response);
        assertNull(response.getSignerName());
        assertNull(response.getSignerEmail());
        assertNull(response.getCertificateVerified());
    }

    @Test
    void testSetSigned() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();

        response.setSigned(true);
        assertTrue(response.isSigned());

        response.setSigned(false);
        assertFalse(response.isSigned());
    }

    @Test
    void testSetSignerName() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();

        response.setSignerName("Jane Smith");
        assertEquals("Jane Smith", response.getSignerName());
    }

    @Test
    void testSetSignerEmail() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();

        response.setSignerEmail("jane@example.com");
        assertEquals("jane@example.com", response.getSignerEmail());
    }

    @Test
    void testSetSignerEdipi() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();

        response.setSignerEdipi("9876543210");
        assertEquals("9876543210", response.getSignerEdipi());
    }

    @Test
    void testSetSignatureTimestamp() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();
        LocalDateTime timestamp = LocalDateTime.now();

        response.setSignatureTimestamp(timestamp);
        assertEquals(timestamp, response.getSignatureTimestamp());
    }

    @Test
    void testSetCertificateIssuer() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();

        response.setCertificateIssuer("CN=Test CA");
        assertEquals("CN=Test CA", response.getCertificateIssuer());
    }

    @Test
    void testSetCertificateSerial() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();

        response.setCertificateSerial("AB:CD:EF:12");
        assertEquals("AB:CD:EF:12", response.getCertificateSerial());
    }

    @Test
    void testSetCertificateNotBefore() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();
        LocalDateTime notBefore = LocalDateTime.now();

        response.setCertificateNotBefore(notBefore);
        assertEquals(notBefore, response.getCertificateNotBefore());
    }

    @Test
    void testSetCertificateNotAfter() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();
        LocalDateTime notAfter = LocalDateTime.now();

        response.setCertificateNotAfter(notAfter);
        assertEquals(notAfter, response.getCertificateNotAfter());
    }

    @Test
    void testSetCertificateVerified() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();

        response.setCertificateVerified(true);
        assertTrue(response.getCertificateVerified());

        response.setCertificateVerified(false);
        assertFalse(response.getCertificateVerified());

        response.setCertificateVerified(null);
        assertNull(response.getCertificateVerified());
    }

    @Test
    void testSetVerificationDate() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();
        LocalDateTime verificationDate = LocalDateTime.now();

        response.setVerificationDate(verificationDate);
        assertEquals(verificationDate, response.getVerificationDate());
    }

    @Test
    void testSetVerificationNotes() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();

        response.setVerificationNotes("Verified by admin");
        assertEquals("Verified by admin", response.getVerificationNotes());
    }

    @Test
    void testSetMessage() {
        SignatureDetailsResponse response = new SignatureDetailsResponse();

        response.setMessage("Verification complete");
        assertEquals("Verification complete", response.getMessage());
    }

    @Test
    void testSignedWithVerifiedCertificate() {
        LocalDateTime now = LocalDateTime.now();

        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                .signed(true)
                .signerName("Test User")
                .signerEmail("test@example.com")
                .signatureTimestamp(now)
                .certificateVerified(true)
                .verificationDate(now)
                .verificationNotes("All checks passed")
                .build();

        assertTrue(response.isSigned());
        assertTrue(response.getCertificateVerified());
        assertNotNull(response.getSignatureTimestamp());
        assertNotNull(response.getVerificationDate());
    }

    @Test
    void testSignedWithUnverifiedCertificate() {
        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                .signed(true)
                .signerName("Test User")
                .certificateVerified(false)
                .verificationNotes("Certificate expired")
                .build();

        assertTrue(response.isSigned());
        assertFalse(response.getCertificateVerified());
        assertEquals("Certificate expired", response.getVerificationNotes());
    }

    @Test
    void testCertificateDateRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusYears(3);
        LocalDateTime future = now.plusYears(2);

        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                .certificateNotBefore(past)
                .certificateNotAfter(future)
                .build();

        assertTrue(response.getCertificateNotBefore().isBefore(response.getCertificateNotAfter()));
        assertTrue(response.getCertificateNotBefore().isBefore(now));
        assertTrue(response.getCertificateNotAfter().isAfter(now));
    }

    @Test
    void testExpiredCertificate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusYears(5);
        LocalDateTime expired = now.minusDays(1);

        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                .signed(true)
                .certificateNotBefore(past)
                .certificateNotAfter(expired)
                .certificateVerified(false)
                .message("Certificate has expired")
                .build();

        assertTrue(response.isSigned());
        assertFalse(response.getCertificateVerified());
        assertTrue(response.getCertificateNotAfter().isBefore(now));
    }

    @Test
    void testMultipleEmailFormats() {
        String[] emails = {
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk"
        };

        for (String email : emails) {
            SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                    .signerEmail(email)
                    .build();
            assertEquals(email, response.getSignerEmail());
        }
    }

    @Test
    void testComplexIssuerDN() {
        String complexDN = "CN=Example Root CA+SERIALNUMBER=123,OU=PKI+OU=Security,O=Example Corp,L=City,ST=State,C=US";

        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                .certificateIssuer(complexDN)
                .build();

        assertEquals(complexDN, response.getCertificateIssuer());
        assertTrue(response.getCertificateIssuer().contains("CN="));
    }

    @Test
    void testLongVerificationNotes() {
        String longNotes = "Certificate verification completed. Checked: validity period (valid), " +
                "signature algorithm (RSA-2048), key usage (digital signature), " +
                "extended key usage (code signing), CRL distribution points (accessible), " +
                "OCSP responder (responded with 'good' status), certificate chain (complete).";

        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                .certificateVerified(true)
                .verificationNotes(longNotes)
                .build();

        assertEquals(longNotes, response.getVerificationNotes());
        assertTrue(response.getVerificationNotes().length() > 100);
    }

    @Test
    void testBuilderChaining() {
        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                .signed(true)
                .signerName("Test")
                .signerEmail("test@example.com")
                .signerEdipi("123")
                .signatureTimestamp(LocalDateTime.now())
                .certificateIssuer("CN=CA")
                .certificateSerial("ABC")
                .certificateNotBefore(LocalDateTime.now())
                .certificateNotAfter(LocalDateTime.now())
                .certificateVerified(true)
                .verificationDate(LocalDateTime.now())
                .verificationNotes("OK")
                .message("Signed")
                .build();

        assertNotNull(response);
        assertTrue(response.isSigned());
        assertNotNull(response.getSignerName());
        assertNotNull(response.getMessage());
    }

    @Test
    void testSetAllFieldsToNull() {
        SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                .signed(true)
                .signerName("Test")
                .certificateVerified(true)
                .build();

        response.setSignerName(null);
        response.setSignerEmail(null);
        response.setSignerEdipi(null);
        response.setSignatureTimestamp(null);
        response.setCertificateIssuer(null);
        response.setCertificateSerial(null);
        response.setCertificateNotBefore(null);
        response.setCertificateNotAfter(null);
        response.setCertificateVerified(null);
        response.setVerificationDate(null);
        response.setVerificationNotes(null);
        response.setMessage(null);

        assertNull(response.getSignerName());
        assertNull(response.getSignerEmail());
        assertNull(response.getSignerEdipi());
        assertNull(response.getSignatureTimestamp());
        assertNull(response.getCertificateIssuer());
        assertNull(response.getCertificateSerial());
        assertNull(response.getCertificateNotBefore());
        assertNull(response.getCertificateNotAfter());
        assertNull(response.getCertificateVerified());
        assertNull(response.getVerificationDate());
        assertNull(response.getVerificationNotes());
        assertNull(response.getMessage());
    }
}
