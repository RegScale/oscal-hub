package gov.nist.oscal.tools.api.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    @Test
    void testValidPassword() {
        String password = "ValidPass123!";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
        assertEquals("", result.getErrorMessage());
    }

    @Test
    void testNullPassword() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate(null);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Password is required", result.getErrors().get(0));
        assertEquals("Password is required", result.getErrorMessage());
    }

    @Test
    void testEmptyPassword() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Password is required", result.getErrors().get(0));
    }

    @Test
    void testPasswordTooShort() {
        String password = "Short1!";  // 7 characters, needs 8

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must be at least 8 characters long"));
    }

    @Test
    void testPasswordExactlyMinLength() {
        String password = "Valid12!";  // Exactly 8 characters

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void testPasswordNoUppercase() {
        String password = "lowercase123!";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one uppercase letter"));
    }

    @Test
    void testPasswordNoLowercase() {
        String password = "UPPERCASE123!";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one lowercase letter"));
    }

    @Test
    void testPasswordNoDigit() {
        String password = "NoDigitsHere!";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one number"));
    }

    @Test
    void testPasswordNoSpecialCharacter() {
        String password = "NoSpecialChar1";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one special character"));
    }

    @Test
    void testPasswordMultipleErrors() {
        String password = "weak";  // Too short, no uppercase, no digit, no special char

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertFalse(result.isValid());
        assertEquals(4, result.getErrors().size());
        assertTrue(result.getErrors().contains("Password must be at least 8 characters long"));
        assertTrue(result.getErrors().contains("Password must contain at least one uppercase letter"));
        assertTrue(result.getErrors().contains("Password must contain at least one number"));
        assertTrue(result.getErrors().contains("Password must contain at least one special character"));
    }

    @Test
    void testPasswordAllErrorsExceptRequired() {
        String password = "abc";  // Too short, no uppercase, no digit, no special char

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertFalse(result.isValid());
        assertEquals(4, result.getErrors().size());
    }

    @Test
    void testGetErrorMessageWithMultipleErrors() {
        String password = "weak";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        String errorMessage = result.getErrorMessage();
        assertNotNull(errorMessage);
        assertFalse(errorMessage.isEmpty());
        assertTrue(errorMessage.contains("Password must be at least 8 characters long"));
        assertTrue(errorMessage.contains("Password must contain at least one uppercase letter"));
    }

    @Test
    void testGetErrorMessageWithNoErrors() {
        String password = "ValidPass123!";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertEquals("", result.getErrorMessage());
    }

    @Test
    void testGetErrorsSeparatedByPeriod() {
        String password = "short";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        String errorMessage = result.getErrorMessage();
        // Errors should be separated by ". "
        assertTrue(errorMessage.contains(". "));
    }

    @Test
    void testValidPasswordWithAllSpecialCharacters() {
        String[] specialChars = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "+",
                                 "-", "=", "[", "]", "{", "}", ";", "'", ":", "\"", "\\",
                                 "|", ",", ".", "<", ">", "/", "?"};

        for (String specialChar : specialChars) {
            String password = "ValidPass1" + specialChar;
            PasswordValidator.ValidationResult result = PasswordValidator.validate(password);
            assertTrue(result.isValid(), "Password with special char '" + specialChar + "' should be valid");
        }
    }

    @Test
    void testPasswordWithMultipleUppercase() {
        String password = "UPPERCASE123!";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        // Should fail because no lowercase
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    void testPasswordWithMultipleLowercase() {
        String password = "lowercase123!";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        // Should fail because no uppercase
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    void testPasswordWithMultipleDigits() {
        String password = "ValidPass123456789!";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertTrue(result.isValid());
    }

    @Test
    void testPasswordWithMultipleSpecialChars() {
        String password = "ValidPass1!@#$%";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertTrue(result.isValid());
    }

    @Test
    void testPasswordWithSpaces() {
        String password = "Valid Pass 1!";  // Contains spaces

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertTrue(result.isValid());  // Spaces should be allowed
    }

    @Test
    void testPasswordWithUnicodeCharacters() {
        String password = "ValidPass1!αβγ";  // Contains Unicode

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertTrue(result.isValid());  // Unicode should be allowed
    }

    @Test
    void testVeryLongPassword() {
        String password = "ValidPass1!" + "a".repeat(100);  // Very long password

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertTrue(result.isValid());
    }

    @Test
    void testValidationResultIsValid() {
        List<String> noErrors = List.of();
        PasswordValidator.ValidationResult result = new PasswordValidator.ValidationResult(true, noErrors);

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void testValidationResultIsNotValid() {
        List<String> errors = List.of("Error 1", "Error 2");
        PasswordValidator.ValidationResult result = new PasswordValidator.ValidationResult(false, errors);

        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertEquals("Error 1. Error 2", result.getErrorMessage());
    }

    @Test
    void testValidationResultGetErrors() {
        List<String> errors = List.of("Error 1", "Error 2", "Error 3");
        PasswordValidator.ValidationResult result = new PasswordValidator.ValidationResult(false, errors);

        List<String> retrievedErrors = result.getErrors();
        assertEquals(3, retrievedErrors.size());
        assertEquals("Error 1", retrievedErrors.get(0));
        assertEquals("Error 2", retrievedErrors.get(1));
        assertEquals("Error 3", retrievedErrors.get(2));
    }

    @Test
    void testPasswordOnlyMissingUppercase() {
        String password = "lowercase1!";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Password must contain at least one uppercase letter", result.getErrors().get(0));
    }

    @Test
    void testPasswordOnlyMissingLowercase() {
        String password = "UPPERCASE1!";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Password must contain at least one lowercase letter", result.getErrors().get(0));
    }

    @Test
    void testPasswordOnlyMissingDigit() {
        String password = "ValidPassword!";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Password must contain at least one number", result.getErrors().get(0));
    }

    @Test
    void testPasswordOnlyMissingSpecialChar() {
        String password = "ValidPassword1";

        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Password must contain at least one special character", result.getErrors().get(0));
    }
}
