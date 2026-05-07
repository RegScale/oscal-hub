/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleUsernameAlreadyExists_returns409WithMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleUsernameAlreadyExists(
                new UsernameAlreadyExistsException("Username already exists"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Username already exists", response.getBody().get("error"));
    }

    @Test
    void handleIllegalArgument_returns400WithMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Password too weak"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Password too weak", response.getBody().get("error"));
    }

    @Test
    void handleDataIntegrityViolation_returns409() {
        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException(
                        "could not execute statement [ERROR: duplicate key value violates unique constraint \"uk6dotkott2kjsp8vw4d0m25fb7\" Detail: Key (email)=(victim@example.com) already exists.]"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleDataIntegrityViolation_responseDoesNotContainOriginalMessage() {
        // This is the security-critical test: the raw Hibernate/JDBC message must
        // never be echoed back to the client.
        String leakyMessage = "could not execute statement [ERROR: duplicate key value violates "
                + "unique constraint \"uk6dotkott2kjsp8vw4d0m25fb7\" Detail: Key (email)="
                + "(victim@example.com) already exists.] [insert into users "
                + "(account_locked_until,city,created_at,email,...) values (?,?,?,?)]";

        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException(leakyMessage));

        String body = response.getBody().get("error");
        assertNotNull(body);
        assertFalse(body.contains("constraint"), "Response leaked the word 'constraint'");
        assertFalse(body.contains("SQL"), "Response leaked SQL details");
        assertFalse(body.contains("insert into"), "Response leaked an INSERT statement");
        assertFalse(body.contains("duplicate key"), "Response leaked a duplicate-key message");
        assertFalse(body.contains("uk6dotkott"), "Response leaked the constraint name hash");
        assertFalse(body.contains("victim@example.com"), "Response leaked another user's email address");
        assertFalse(body.contains("users"), "Response leaked the table name");
    }

    @Test
    void handleGeneric_returns500WithGenericMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleGeneric(
                new RuntimeException("ConnectException: stack trace details ..."));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody().get("error"));
    }

    @Test
    void handleGeneric_responseDoesNotContainOriginalMessage() {
        String leakyMessage = "java.sql.SQLException: connection refused at jdbc:postgresql://internal-host:5432/proddb";

        ResponseEntity<Map<String, String>> response = handler.handleGeneric(
                new RuntimeException(leakyMessage));

        String body = response.getBody().get("error");
        assertNotNull(body);
        assertFalse(body.contains("SQLException"));
        assertFalse(body.contains("jdbc:"));
        assertFalse(body.contains("internal-host"));
        assertFalse(body.contains("proddb"));
    }

    @Test
    void handleAccessDenied_returns403() {
        ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(
                new AccessDeniedException("Access denied for user with role USER"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access denied", response.getBody().get("error"));
        // Original message must not leak
        assertFalse(response.getBody().get("error").contains("USER"));
    }

    @Test
    void handleAuthentication_returns401() {
        ResponseEntity<Map<String, String>> response = handler.handleAuthentication(
                new BadCredentialsException("Bad credentials for principal foo"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Not authenticated", response.getBody().get("error"));
        // Original message must not leak
        assertFalse(response.getBody().get("error").contains("foo"));
    }

    @Test
    void handleGeneric_handlesNullPointerException() {
        // NullPointerException is a RuntimeException — must also map to 500 generic, not leak.
        ResponseEntity<Map<String, String>> response = handler.handleGeneric(
                new NullPointerException("Cannot invoke getId() on null"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String body = response.getBody().get("error");
        assertFalse(body.contains("getId"));
        assertFalse(body.contains("null"));
    }
}
