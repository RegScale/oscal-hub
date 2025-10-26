package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SignatureResultTest {

    @Test
    void testNoArgsConstructor() {
        SignatureResult result = new SignatureResult();

        assertNotNull(result);
        assertFalse(result.isSuccess()); // primitive boolean defaults to false
        assertNull(result.getSignerName());
        assertNull(result.getSignerEmail());
        assertNull(result.getSignerEdipi());
        assertNull(result.getSignatureTimestamp());
        assertNull(result.getMessage());
    }

    @Test
    void testTwoArgsConstructor() {
        boolean success = true;
        String message = "Signature created successfully";

        SignatureResult result = new SignatureResult(success, message);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(message, result.getMessage());
        assertNull(result.getSignerName());
        assertNull(result.getSignerEmail());
        assertNull(result.getSignerEdipi());
        assertNull(result.getSignatureTimestamp());
    }

    @Test
    void testTwoArgsConstructorWithFailure() {
        boolean success = false;
        String message = "Certificate validation failed";

        SignatureResult result = new SignatureResult(success, message);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(message, result.getMessage());
    }

    @Test
    void testBuilderWithAllFields() {
        boolean success = true;
        String signerName = "John Doe";
        String signerEmail = "john.doe@example.com";
        String signerEdipi = "1234567890";
        LocalDateTime timestamp = LocalDateTime.now();
        String message = "Document signed successfully";

        SignatureResult result = SignatureResult.builder()
                .success(success)
                .signerName(signerName)
                .signerEmail(signerEmail)
                .signerEdipi(signerEdipi)
                .signatureTimestamp(timestamp)
                .message(message)
                .build();

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(signerName, result.getSignerName());
        assertEquals(signerEmail, result.getSignerEmail());
        assertEquals(signerEdipi, result.getSignerEdipi());
        assertEquals(timestamp, result.getSignatureTimestamp());
        assertEquals(message, result.getMessage());
    }

    @Test
    void testBuilderWithMinimalFields() {
        boolean success = false;
        String message = "Error occurred";

        SignatureResult result = SignatureResult.builder()
                .success(success)
                .message(message)
                .build();

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(message, result.getMessage());
        assertNull(result.getSignerName());
        assertNull(result.getSignerEmail());
        assertNull(result.getSignerEdipi());
        assertNull(result.getSignatureTimestamp());
    }

    @Test
    void testSetSuccess() {
        SignatureResult result = new SignatureResult();

        result.setSuccess(true);
        assertTrue(result.isSuccess());

        result.setSuccess(false);
        assertFalse(result.isSuccess());
    }

    @Test
    void testSetSignerName() {
        SignatureResult result = new SignatureResult();
        String signerName = "Jane Smith";

        result.setSignerName(signerName);

        assertEquals(signerName, result.getSignerName());
    }

    @Test
    void testSetSignerEmail() {
        SignatureResult result = new SignatureResult();
        String signerEmail = "jane.smith@example.com";

        result.setSignerEmail(signerEmail);

        assertEquals(signerEmail, result.getSignerEmail());
    }

    @Test
    void testSetSignerEdipi() {
        SignatureResult result = new SignatureResult();
        String signerEdipi = "9876543210";

        result.setSignerEdipi(signerEdipi);

        assertEquals(signerEdipi, result.getSignerEdipi());
    }

    @Test
    void testSetSignatureTimestamp() {
        SignatureResult result = new SignatureResult();
        LocalDateTime timestamp = LocalDateTime.now();

        result.setSignatureTimestamp(timestamp);

        assertEquals(timestamp, result.getSignatureTimestamp());
    }

    @Test
    void testSetMessage() {
        SignatureResult result = new SignatureResult();
        String message = "Signature verification completed";

        result.setMessage(message);

        assertEquals(message, result.getMessage());
    }

    @Test
    void testSetAllFieldsToNull() {
        SignatureResult result = SignatureResult.builder()
                .success(true)
                .signerName("Test")
                .signerEmail("test@example.com")
                .signerEdipi("123")
                .signatureTimestamp(LocalDateTime.now())
                .message("Test message")
                .build();

        // Set all nullable fields to null (success is a primitive boolean and can't be null)
        result.setSignerName(null);
        result.setSignerEmail(null);
        result.setSignerEdipi(null);
        result.setSignatureTimestamp(null);
        result.setMessage(null);

        assertNull(result.getSignerName());
        assertNull(result.getSignerEmail());
        assertNull(result.getSignerEdipi());
        assertNull(result.getSignatureTimestamp());
        assertNull(result.getMessage());
    }

    @Test
    void testBuilderWithNullValues() {
        SignatureResult result = SignatureResult.builder()
                .signerName(null)
                .signerEmail(null)
                .signerEdipi(null)
                .signatureTimestamp(null)
                .message(null)
                .build();

        assertNotNull(result);
        assertFalse(result.isSuccess()); // primitive boolean defaults to false
        assertNull(result.getSignerName());
        assertNull(result.getSignerEmail());
        assertNull(result.getSignerEdipi());
        assertNull(result.getSignatureTimestamp());
        assertNull(result.getMessage());
    }

    @Test
    void testBuilderWithEmptyStrings() {
        SignatureResult result = SignatureResult.builder()
                .signerName("")
                .signerEmail("")
                .signerEdipi("")
                .message("")
                .build();

        assertNotNull(result);
        assertEquals("", result.getSignerName());
        assertEquals("", result.getSignerEmail());
        assertEquals("", result.getSignerEdipi());
        assertEquals("", result.getMessage());
    }

    @Test
    void testSuccessfulSignatureScenario() {
        LocalDateTime now = LocalDateTime.now();

        SignatureResult result = SignatureResult.builder()
                .success(true)
                .signerName("John Doe")
                .signerEmail("john.doe@example.com")
                .signerEdipi("1234567890")
                .signatureTimestamp(now)
                .message("Authorization signed successfully with certificate CN=John Doe")
                .build();

        assertTrue(result.isSuccess());
        assertNotNull(result.getSignerName());
        assertNotNull(result.getSignerEmail());
        assertNotNull(result.getSignerEdipi());
        assertNotNull(result.getSignatureTimestamp());
        assertTrue(result.getMessage().contains("successfully"));
    }

    @Test
    void testFailedSignatureScenario() {
        SignatureResult result = SignatureResult.builder()
                .success(false)
                .message("Certificate validation failed: Certificate has expired")
                .build();

        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("failed"));
        assertNull(result.getSignerName());
        assertNull(result.getSignatureTimestamp());
    }

    @Test
    void testMultipleEmailFormats() {
        String[] emails = {
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk",
                "user_name@sub.example.com"
        };

        for (String email : emails) {
            SignatureResult result = SignatureResult.builder()
                    .signerEmail(email)
                    .build();
            assertEquals(email, result.getSignerEmail());
        }
    }

    @Test
    void testVariousMessageTypes() {
        String[] messages = {
                "Signature created successfully",
                "Certificate validation failed",
                "Authorization not found",
                "Invalid certificate format",
                "Signature verification completed"
        };

        for (String message : messages) {
            SignatureResult result = new SignatureResult(true, message);
            assertEquals(message, result.getMessage());
        }
    }

    @Test
    void testTimestampPrecision() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusDays(1);
        LocalDateTime future = now.plusDays(1);

        SignatureResult result1 = SignatureResult.builder()
                .signatureTimestamp(past)
                .build();
        SignatureResult result2 = SignatureResult.builder()
                .signatureTimestamp(now)
                .build();
        SignatureResult result3 = SignatureResult.builder()
                .signatureTimestamp(future)
                .build();

        assertTrue(result1.getSignatureTimestamp().isBefore(result2.getSignatureTimestamp()));
        assertTrue(result2.getSignatureTimestamp().isBefore(result3.getSignatureTimestamp()));
    }

    @Test
    void testBuilderChaining() {
        SignatureResult result = SignatureResult.builder()
                .success(true)
                .signerName("Test User")
                .signerEmail("test@example.com")
                .signerEdipi("1234567890")
                .signatureTimestamp(LocalDateTime.now())
                .message("Test message")
                .build();

        assertNotNull(result);
        assertTrue(result.isSuccess()); // success is primitive boolean, can't be null
        assertNotNull(result.getSignerName());
        assertNotNull(result.getSignerEmail());
        assertNotNull(result.getSignerEdipi());
        assertNotNull(result.getSignatureTimestamp());
        assertNotNull(result.getMessage());
    }

    @Test
    void testUpdateExistingSignatureResult() {
        SignatureResult result = new SignatureResult(false, "Initial error");

        // Update to success
        result.setSuccess(true);
        result.setMessage("Signature completed after retry");
        result.setSignerName("John Doe");
        result.setSignerEmail("john@example.com");
        result.setSignerEdipi("1234567890");
        result.setSignatureTimestamp(LocalDateTime.now());

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("completed"));
        assertNotNull(result.getSignerName());
        assertNotNull(result.getSignatureTimestamp());
    }

    @Test
    void testLongMessageContent() {
        String longMessage = "Signature operation failed due to multiple validation errors: " +
                "Certificate has expired, certificate chain is incomplete, " +
                "issuer is not trusted, and signature algorithm is not supported.";

        SignatureResult result = SignatureResult.builder()
                .success(false)
                .message(longMessage)
                .build();

        assertEquals(longMessage, result.getMessage());
        assertTrue(result.getMessage().length() > 100);
    }

    @Test
    void testSpecialCharactersInFields() {
        String nameWithSpecialChars = "O'Brien, Jr.";
        String emailWithPlus = "user+test@example.com";
        String messageWithQuotes = "Error: \"Invalid certificate\" detected";

        SignatureResult result = SignatureResult.builder()
                .signerName(nameWithSpecialChars)
                .signerEmail(emailWithPlus)
                .message(messageWithQuotes)
                .build();

        assertEquals(nameWithSpecialChars, result.getSignerName());
        assertEquals(emailWithPlus, result.getSignerEmail());
        assertEquals(messageWithQuotes, result.getMessage());
    }

    @Test
    void testEdipiFormats() {
        String[] edipis = {
                "1234567890",
                "0000000001",
                "9999999999"
        };

        for (String edipi : edipis) {
            SignatureResult result = SignatureResult.builder()
                    .signerEdipi(edipi)
                    .build();
            assertEquals(edipi, result.getSignerEdipi());
        }
    }
}
