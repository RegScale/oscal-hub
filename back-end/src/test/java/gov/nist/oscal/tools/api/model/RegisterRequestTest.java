package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    @Test
    void testNoArgsConstructor() {
        RegisterRequest request = new RegisterRequest();

        assertNotNull(request);
        assertNull(request.getUsername());
        assertNull(request.getPassword());
        assertNull(request.getEmail());
    }

    @Test
    void testAllArgsConstructor() {
        RegisterRequest request = new RegisterRequest("testuser", "password123", "test@example.com");

        assertEquals("testuser", request.getUsername());
        assertEquals("password123", request.getPassword());
        assertEquals("test@example.com", request.getEmail());
    }

    @Test
    void testSetUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        assertEquals("john_doe", request.getUsername());
    }

    @Test
    void testSetPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setPassword("securePassword123");
        assertEquals("securePassword123", request.getPassword());
    }

    @Test
    void testSetEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john.doe@example.com");
        assertEquals("john.doe@example.com", request.getEmail());
    }

    @Test
    void testSetAllFields() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin");
        request.setPassword("adminPass123");
        request.setEmail("admin@system.com");

        assertEquals("admin", request.getUsername());
        assertEquals("adminPass123", request.getPassword());
        assertEquals("admin@system.com", request.getEmail());
    }

    @Test
    void testSetFieldsToNull() {
        RegisterRequest request = new RegisterRequest("user", "pass", "email@test.com");
        request.setUsername(null);
        request.setPassword(null);
        request.setEmail(null);

        assertNull(request.getUsername());
        assertNull(request.getPassword());
        assertNull(request.getEmail());
    }

    @Test
    void testModifyAllFields() {
        RegisterRequest request = new RegisterRequest();

        request.setUsername("firstUser");
        request.setPassword("firstPass");
        request.setEmail("first@example.com");

        request.setUsername("secondUser");
        request.setPassword("secondPass");
        request.setEmail("second@example.com");

        assertEquals("secondUser", request.getUsername());
        assertEquals("secondPass", request.getPassword());
        assertEquals("second@example.com", request.getEmail());
    }

    @Test
    void testWithEmptyStrings() {
        RegisterRequest request = new RegisterRequest("", "", "");

        assertEquals("", request.getUsername());
        assertEquals("", request.getPassword());
        assertEquals("", request.getEmail());
    }

    @Test
    void testWithMinimumValidUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("abc"); // Minimum 3 characters
        assertEquals("abc", request.getUsername());
    }

    @Test
    void testWithMinimumValidPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setPassword("12345678"); // Minimum 8 characters
        assertEquals("12345678", request.getPassword());
    }

    @Test
    void testWithValidEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        assertEquals("user@example.com", request.getEmail());
    }

    @Test
    void testWithLongUsername() {
        String longUsername = "a".repeat(50); // Maximum 50 characters
        RegisterRequest request = new RegisterRequest();
        request.setUsername(longUsername);
        assertEquals(longUsername, request.getUsername());
    }

    @Test
    void testWithVeryLongPassword() {
        String longPassword = "a".repeat(100);
        RegisterRequest request = new RegisterRequest();
        request.setPassword(longPassword);
        assertEquals(longPassword, request.getPassword());
    }

    @Test
    void testWithSpecialCharactersInUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user_name-123");
        assertEquals("user_name-123", request.getUsername());
    }

    @Test
    void testWithSpecialCharactersInPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setPassword("P@ssw0rd!#$%");
        assertEquals("P@ssw0rd!#$%", request.getPassword());
    }

    @Test
    void testWithComplexEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user.name+tag@subdomain.example.com");
        assertEquals("user.name+tag@subdomain.example.com", request.getEmail());
    }

    @Test
    void testConstructorAndSettersCombined() {
        RegisterRequest request = new RegisterRequest("initial", "initialPass", "initial@test.com");

        assertEquals("initial", request.getUsername());
        assertEquals("initialPass", request.getPassword());
        assertEquals("initial@test.com", request.getEmail());

        request.setUsername("updated");
        request.setPassword("updatedPass");
        request.setEmail("updated@test.com");

        assertEquals("updated", request.getUsername());
        assertEquals("updatedPass", request.getPassword());
        assertEquals("updated@test.com", request.getEmail());
    }

    @Test
    void testCompleteRegistrationScenario() {
        RegisterRequest request = new RegisterRequest(
            "johndoe",
            "SecureP@ss123",
            "john.doe@example.com"
        );

        assertNotNull(request);
        assertTrue(request.getUsername().length() >= 3);
        assertTrue(request.getPassword().length() >= 8);
        assertTrue(request.getEmail().contains("@"));
    }

    @Test
    void testWithWhitespaceInFields() {
        RegisterRequest request = new RegisterRequest(
            " user ",
            " pass ",
            " email@test.com "
        );

        assertEquals(" user ", request.getUsername());
        assertEquals(" pass ", request.getPassword());
        assertEquals(" email@test.com ", request.getEmail());
    }
}
