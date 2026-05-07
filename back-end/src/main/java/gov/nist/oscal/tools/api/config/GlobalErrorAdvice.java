package gov.nist.oscal.tools.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class GlobalErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorAdvice.class);

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
     * Catch-all for anything else that isn't already handled. Logs the full
     * stack trace and returns the message in the response body.
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
        // Let @ResponseStatus-annotated exceptions pass through to Spring's
        // ResponseStatusExceptionResolver which reads the annotation value.
        if (e.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.ResponseStatus.class)) {
            throw e;
        }
        log.error("500 Internal Server Error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal Server Error",
                        "message", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()
                ));
    }
}
