package gov.nist.oscal.tools.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handling for the API.
 *
 * The primary purpose of this class is to ensure that internal exception details
 * (SQL fragments, table/column names, constraint names, stack traces) are never
 * exposed to API clients. All such details are logged server-side instead.
 *
 * Friendly, user-facing messages are returned only for exception types that
 * are explicitly part of the API contract.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Spring Security throws {@link AccessDeniedException} when an authenticated user
     * fails an authorization check (e.g. {@code @PreAuthorize}). Without this explicit
     * handler the catch-all below would map it to 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "Access denied");
    }

    /**
     * Spring Security {@link AuthenticationException} for failed/missing authentication.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Not authenticated");
    }

    /**
     * Catch-all for database constraint violations that escape service-layer pre-checks
     * (e.g., a race condition between a uniqueness check and the insert).
     *
     * The original Hibernate/JDBC message must NEVER be returned to the client because
     * it leaks SQL, table names, and constraint identifiers.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.warn("Database integrity violation", ex);
        return error(HttpStatus.CONFLICT,
                "The requested operation conflicts with existing data.");
    }

    /**
     * Catch-all for unexpected runtime exceptions. Returns a generic 500 to ensure no
     * internal details leak. The full exception is logged server-side for diagnosis.
     *
     * Scoped to {@link RuntimeException} (not {@link Exception}) so that Spring's
     * default handlers continue to manage checked exceptions like
     * {@code MethodArgumentNotValidException} (bean-validation failures → 400) and
     * {@code HttpMessageNotReadableException} (malformed JSON → 400).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleGeneric(RuntimeException ex) {
        logger.error("Unhandled exception in API request", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal error occurred. Please try again later.");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
