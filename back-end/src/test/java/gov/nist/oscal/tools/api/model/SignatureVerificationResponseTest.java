package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SignatureVerificationResponseTest {

    @Test
    void testNoArgsConstructor() {
        SignatureVerificationResponse response = new SignatureVerificationResponse();

        assertNotNull(response);
        assertFalse(response.isValid());
        assertNull(response.getVerificationDate());
        assertNull(response.getNotes());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime verificationTime = LocalDateTime.of(2025, 1, 15, 10, 30);
        SignatureVerificationResponse response = new SignatureVerificationResponse(
                true,
                verificationTime,
                "Certificate validated successfully against DoD PKI root"
        );

        assertTrue(response.isValid());
        assertEquals(verificationTime, response.getVerificationDate());
        assertTrue(response.getNotes().contains("DoD PKI"));
    }

    @Test
    void testAllArgsConstructorWithInvalidSignature() {
        LocalDateTime verificationTime = LocalDateTime.of(2025, 1, 20, 14, 45);
        SignatureVerificationResponse response = new SignatureVerificationResponse(
                false,
                verificationTime,
                "Certificate has expired"
        );

        assertFalse(response.isValid());
        assertEquals(verificationTime, response.getVerificationDate());
        assertEquals("Certificate has expired", response.getNotes());
    }

    @Test
    void testBuilder() {
        LocalDateTime verificationTime = LocalDateTime.of(2025, 2, 10, 9, 0);
        SignatureVerificationResponse response = SignatureVerificationResponse.builder()
                .valid(true)
                .verificationDate(verificationTime)
                .notes("Signature verification completed")
                .build();

        assertTrue(response.isValid());
        assertEquals(verificationTime, response.getVerificationDate());
        assertEquals("Signature verification completed", response.getNotes());
    }

    @Test
    void testBuilderWithInvalidSignature() {
        LocalDateTime verificationTime = LocalDateTime.now();
        SignatureVerificationResponse response = SignatureVerificationResponse.builder()
                .valid(false)
                .verificationDate(verificationTime)
                .notes("Certificate chain validation failed")
                .build();

        assertFalse(response.isValid());
        assertNotNull(response.getVerificationDate());
        assertTrue(response.getNotes().contains("failed"));
    }

    @Test
    void testBuilderWithNullNotes() {
        SignatureVerificationResponse response = SignatureVerificationResponse.builder()
                .valid(true)
                .verificationDate(LocalDateTime.now())
                .notes(null)
                .build();

        assertTrue(response.isValid());
        assertNull(response.getNotes());
    }

    @Test
    void testSetValid() {
        SignatureVerificationResponse response = new SignatureVerificationResponse();
        assertFalse(response.isValid());

        response.setValid(true);
        assertTrue(response.isValid());

        response.setValid(false);
        assertFalse(response.isValid());
    }

    @Test
    void testSetVerificationDate() {
        SignatureVerificationResponse response = new SignatureVerificationResponse();
        LocalDateTime timestamp = LocalDateTime.of(2025, 3, 25, 11, 15);
        response.setVerificationDate(timestamp);
        assertEquals(timestamp, response.getVerificationDate());
    }

    @Test
    void testSetNotes() {
        SignatureVerificationResponse response = new SignatureVerificationResponse();
        String notes = "Certificate verified against trusted root CA";
        response.setNotes(notes);
        assertEquals(notes, response.getNotes());
    }

    @Test
    void testSetAllFieldsToNull() {
        SignatureVerificationResponse response = new SignatureVerificationResponse();
        response.setValid(true);
        response.setVerificationDate(LocalDateTime.now());
        response.setNotes("notes");

        response.setValid(false);
        response.setVerificationDate(null);
        response.setNotes(null);

        assertFalse(response.isValid());
        assertNull(response.getVerificationDate());
        assertNull(response.getNotes());
    }

    @Test
    void testModifyAllFields() {
        SignatureVerificationResponse response = new SignatureVerificationResponse();

        response.setValid(true);
        response.setVerificationDate(LocalDateTime.of(2025, 1, 1, 0, 0));
        response.setNotes("Old notes");

        response.setValid(false);
        response.setVerificationDate(LocalDateTime.of(2025, 6, 1, 0, 0));
        response.setNotes("New notes");

        assertFalse(response.isValid());
        assertEquals(LocalDateTime.of(2025, 6, 1, 0, 0), response.getVerificationDate());
        assertEquals("New notes", response.getNotes());
    }

    @Test
    void testWithLongNotes() {
        SignatureVerificationResponse response = new SignatureVerificationResponse();
        StringBuilder longNotes = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longNotes.append("Certificate validation step ").append(i).append(" completed successfully. ");
        }
        response.setNotes(longNotes.toString());
        assertTrue(response.getNotes().length() > 1000);
    }

    @Test
    void testWithEmptyNotes() {
        SignatureVerificationResponse response = new SignatureVerificationResponse();
        response.setNotes("");
        assertEquals("", response.getNotes());
    }

    @Test
    void testSuccessfulVerificationScenario() {
        LocalDateTime verificationTime = LocalDateTime.of(2025, 4, 15, 14, 30);
        SignatureVerificationResponse response = SignatureVerificationResponse.builder()
                .valid(true)
                .verificationDate(verificationTime)
                .notes("Certificate successfully verified. Valid until 2027-04-15. Issued by DoD Root CA 3.")
                .build();

        assertTrue(response.isValid());
        assertNotNull(response.getVerificationDate());
        assertTrue(response.getNotes().contains("successfully"));
        assertTrue(response.getNotes().contains("DoD"));
    }

    @Test
    void testFailedVerificationScenario() {
        LocalDateTime verificationTime = LocalDateTime.of(2025, 5, 20, 9, 45);
        SignatureVerificationResponse response = SignatureVerificationResponse.builder()
                .valid(false)
                .verificationDate(verificationTime)
                .notes("Certificate validation failed: Certificate expired on 2024-12-31. Please renew.")
                .build();

        assertFalse(response.isValid());
        assertNotNull(response.getVerificationDate());
        assertTrue(response.getNotes().contains("failed"));
        assertTrue(response.getNotes().contains("expired"));
    }

    @Test
    void testBuilderChaining() {
        SignatureVerificationResponse response = SignatureVerificationResponse.builder()
                .valid(true)
                .verificationDate(LocalDateTime.of(2025, 6, 30, 16, 0))
                .notes("Verification successful")
                .build();

        assertTrue(response.isValid());
        assertEquals(LocalDateTime.of(2025, 6, 30, 16, 0), response.getVerificationDate());
        assertEquals("Verification successful", response.getNotes());
    }

    @Test
    void testBuilderCanBuildMultipleTimes() {
        SignatureVerificationResponse.Builder builder = SignatureVerificationResponse.builder();

        SignatureVerificationResponse response1 = builder
                .valid(true)
                .verificationDate(LocalDateTime.now())
                .notes("First verification")
                .build();

        SignatureVerificationResponse response2 = builder
                .valid(false)
                .verificationDate(LocalDateTime.now())
                .notes("Second verification")
                .build();

        // Both responses should be created successfully
        assertNotNull(response1);
        assertNotNull(response2);
        // Note: Builder pattern may share state - this tests that build() works multiple times
    }

    @Test
    void testWithPastVerificationDate() {
        SignatureVerificationResponse response = new SignatureVerificationResponse();
        LocalDateTime pastDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        response.setVerificationDate(pastDate);
        assertTrue(response.getVerificationDate().isBefore(LocalDateTime.now()));
    }

    @Test
    void testWithFutureVerificationDate() {
        SignatureVerificationResponse response = new SignatureVerificationResponse();
        LocalDateTime futureDate = LocalDateTime.of(2030, 12, 31, 23, 59);
        response.setVerificationDate(futureDate);
        assertTrue(response.getVerificationDate().isAfter(LocalDateTime.now()));
    }

    @Test
    void testValidTrueAndFalse() {
        SignatureVerificationResponse validResponse = new SignatureVerificationResponse();
        validResponse.setValid(true);
        assertTrue(validResponse.isValid());

        SignatureVerificationResponse invalidResponse = new SignatureVerificationResponse();
        invalidResponse.setValid(false);
        assertFalse(invalidResponse.isValid());
    }

    @Test
    void testWithSpecialCharactersInNotes() {
        SignatureVerificationResponse response = new SignatureVerificationResponse();
        String notesWithSpecialChars = "Cert DN: CN=John Doe.1234567890, OU=PKI, O=DoD, C=US (validated @ 2025-01-15)";
        response.setNotes(notesWithSpecialChars);
        assertEquals(notesWithSpecialChars, response.getNotes());
        assertTrue(response.getNotes().contains("@"));
        assertTrue(response.getNotes().contains("="));
    }

    @Test
    void testMultipleBuilderInstances() {
        SignatureVerificationResponse response1 = SignatureVerificationResponse.builder()
                .valid(true)
                .verificationDate(LocalDateTime.of(2025, 1, 1, 10, 0))
                .notes("First")
                .build();

        SignatureVerificationResponse response2 = SignatureVerificationResponse.builder()
                .valid(false)
                .verificationDate(LocalDateTime.of(2025, 2, 1, 11, 0))
                .notes("Second")
                .build();

        assertTrue(response1.isValid());
        assertFalse(response2.isValid());
        assertEquals("First", response1.getNotes());
        assertEquals("Second", response2.getNotes());
    }
}
