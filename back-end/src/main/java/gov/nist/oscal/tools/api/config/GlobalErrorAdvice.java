package gov.nist.oscal.tools.api.config;

import gov.nist.oscal.tools.api.exception.UsernameAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Surfaces uncaught exceptions as JSON response bodies so clients can show
 * actionable messages instead of empty 500s. Spring's default handler returns
 * an empty body when error.include-message is on its default value.
 *
 * Order: this advice runs LAST. Exceptions annotated with @ResponseStatus or
 * thrown as ResponseStatusException are still handled by their own annotations.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorAdvice.class);

    /**
     * Duplicate username on /api/auth/register → 409 Conflict. The flat
     * {@code {"error": "<msg>"}} shape matches the contract the frontend
     * api-client uses for registration failures (api-client.ts reads
     * {@code error.error}).
     */
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameAlreadyExists(UsernameAlreadyExistsException e) {
        log.warn("409 Conflict: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", e.getMessage() == null ? "Username already exists" : e.getMessage()
                ));
    }

    /**
     * Database constraint violation that escapes service-layer pre-checks
     * (e.g., a race between a uniqueness check and the insert). Returns a
     * generic 409 — the original Hibernate/JDBC message MUST NOT leak to the
     * client because it exposes SQL, table names, and constraint identifiers.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("409 Conflict — database integrity violation", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "The requested operation conflicts with existing data."
                ));
    }

    /**
     * Bean-validation failures (@NotBlank, @Size, @Email on request DTOs) → 400
     * with the first field message in the body. Without this handler Spring's
     * default rendering returns {"error":"Bad Request"} with NO message field,
     * so the frontend could only show a bare "Bad Request" — the exact failure
     * mode that made the July 2026 registration errors unreadable.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            org.springframework.web.bind.MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("Invalid request");
        log.warn("400 Bad Request (validation): {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Bad Request",
                        "message", message == null ? "Invalid request" : message
                ));
    }

    /**
     * IllegalArgumentException → 400 with the message in the body.
     * Common case: bad input that we throw without using ResponseStatusException.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("400 Bad Request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Bad Request",
                        "message", e.getMessage() == null ? "Invalid argument" : e.getMessage()
                ));
    }

    /**
     * Spring Security's AccessDeniedException (and AuthorizationDeniedException, which
     * extends it) signals a denied authorization decision — including those raised by
     * @PreAuthorize. Without this explicit handler the catch-all RuntimeException
     * handler below would convert it to 500, masking authorization failures as server
     * errors and breaking front-end retry/redirect logic that watches for 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        log.warn("403 Forbidden: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "Forbidden",
                        "message", e.getMessage() == null ? "Access denied" : e.getMessage()
                ));
    }

    /**
     * Authentication failure thrown after the security filter chain (e.g. by application
     * code) — return 401 instead of letting the catch-all turn it into 500.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException e) {
        log.warn("401 Unauthorized: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "Unauthorized",
                        "message", e.getMessage() == null ? "Authentication required" : e.getMessage()
                ));
    }

    /**
     * Catch-all for anything else that isn't already handled. Logs the full
     * stack trace server-side and returns a generic message — the original
     * exception text MUST NOT be returned because it can leak SQL fragments,
     * connection strings, internal hostnames, and PII.
     *
     * NB: ResponseStatusException is re-thrown unchanged so Spring's default
     * mapping (which respects the carried status) takes effect.
     *
     * NB: Exceptions annotated with @ResponseStatus are also re-thrown so that
     * Spring's ResponseStatusExceptionResolver can map them to the correct HTTP
     * status code declared in the annotation (e.g. NOT_FOUND, FORBIDDEN, etc.).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) throws RuntimeException {
        if (e instanceof ResponseStatusException) {
            throw e;
        }
        if (e.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.ResponseStatus.class)) {
            throw e;
        }
        log.error("500 Internal Server Error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal Server Error",
                        "message", "An internal error occurred. Please try again later."
                ));
    }
}
