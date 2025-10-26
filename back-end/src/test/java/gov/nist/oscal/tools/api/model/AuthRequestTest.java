package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthRequestTest {

    @Test
    void testNoArgsConstructor() {
        AuthRequest request = new AuthRequest();

        assertNotNull(request);
        assertNull(request.getUsername());
        assertNull(request.getPassword());
    }

    @Test
    void testAllArgsConstructor() {
        AuthRequest request = new AuthRequest("admin", "password123");

        assertEquals("admin", request.getUsername());
        assertEquals("password123", request.getPassword());
    }

    @Test
    void testSetUsername() {
        AuthRequest request = new AuthRequest();
        request.setUsername("john.doe");
        assertEquals("john.doe", request.getUsername());
    }

    @Test
    void testSetPassword() {
        AuthRequest request = new AuthRequest();
        request.setPassword("securePassword!");
        assertEquals("securePassword!", request.getPassword());
    }

    @Test
    void testSetAllFields() {
        AuthRequest request = new AuthRequest();
        request.setUsername("alice");
        request.setPassword("alice123");

        assertEquals("alice", request.getUsername());
        assertEquals("alice123", request.getPassword());
    }

    @Test
    void testSetFieldsToNull() {
        AuthRequest request = new AuthRequest("user", "pass");

        request.setUsername(null);
        request.setPassword(null);

        assertNull(request.getUsername());
        assertNull(request.getPassword());
    }

    @Test
    void testModifyAllFields() {
        AuthRequest request = new AuthRequest();

        request.setUsername("oldUser");
        request.setPassword("oldPass");

        request.setUsername("newUser");
        request.setPassword("newPass");

        assertEquals("newUser", request.getUsername());
        assertEquals("newPass", request.getPassword());
    }

    @Test
    void testWithEmptyStrings() {
        AuthRequest request = new AuthRequest("", "");

        assertEquals("", request.getUsername());
        assertEquals("", request.getPassword());
    }

    @Test
    void testWithSpecialCharactersInUsername() {
        AuthRequest request = new AuthRequest();
        String specialUsername = "user@example.com";
        request.setUsername(specialUsername);
        assertEquals(specialUsername, request.getUsername());
    }

    @Test
    void testWithSpecialCharactersInPassword() {
        AuthRequest request = new AuthRequest();
        String complexPassword = "P@ssw0rd!#$%^&*()";
        request.setPassword(complexPassword);
        assertEquals(complexPassword, request.getPassword());
    }

    @Test
    void testWithLongUsername() {
        AuthRequest request = new AuthRequest();
        StringBuilder longUsername = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longUsername.append("user");
        }
        request.setUsername(longUsername.toString());
        assertEquals(longUsername.toString(), request.getUsername());
    }

    @Test
    void testWithLongPassword() {
        AuthRequest request = new AuthRequest();
        StringBuilder longPassword = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longPassword.append("pass");
        }
        request.setPassword(longPassword.toString());
        assertEquals(longPassword.toString(), request.getPassword());
    }

    @Test
    void testWithWhitespaceInFields() {
        AuthRequest request = new AuthRequest("  user  ", "  pass  ");
        assertEquals("  user  ", request.getUsername());
        assertEquals("  pass  ", request.getPassword());
    }

    @Test
    void testTypicalLoginScenario() {
        AuthRequest request = new AuthRequest("john.smith@company.com", "MySecure123!");

        assertNotNull(request.getUsername());
        assertNotNull(request.getPassword());
        assertTrue(request.getUsername().contains("@"));
        assertTrue(request.getPassword().length() > 8);
    }

    @Test
    void testWithUsernameOnly() {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");

        assertEquals("admin", request.getUsername());
        assertNull(request.getPassword());
    }

    @Test
    void testWithPasswordOnly() {
        AuthRequest request = new AuthRequest();
        request.setPassword("secret123");

        assertNull(request.getUsername());
        assertEquals("secret123", request.getPassword());
    }

    @Test
    void testConstructorAndSettersCombined() {
        AuthRequest request = new AuthRequest("initialUser", "initialPass");

        assertEquals("initialUser", request.getUsername());
        assertEquals("initialPass", request.getPassword());

        request.setUsername("updatedUser");
        request.setPassword("updatedPass");

        assertEquals("updatedUser", request.getUsername());
        assertEquals("updatedPass", request.getPassword());
    }

    @Test
    void testWithNumericUsername() {
        AuthRequest request = new AuthRequest("12345", "password");
        assertEquals("12345", request.getUsername());
    }

    @Test
    void testCaseSensitivity() {
        AuthRequest request1 = new AuthRequest("Admin", "Password");
        AuthRequest request2 = new AuthRequest("admin", "password");

        assertNotEquals(request1.getUsername(), request2.getUsername());
        assertNotEquals(request1.getPassword(), request2.getPassword());
    }

    @Test
    void testWithUnicodeCharacters() {
        AuthRequest request = new AuthRequest("用户", "密码123");
        assertEquals("用户", request.getUsername());
        assertEquals("密码123", request.getPassword());
    }
}
