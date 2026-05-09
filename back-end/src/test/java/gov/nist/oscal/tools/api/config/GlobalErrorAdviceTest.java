package gov.nist.oscal.tools.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for GlobalErrorAdvice — the @ControllerAdvice that maps
 * uncaught exceptions to HTTP responses.
 *
 * Particular focus: ensure AccessDeniedException (and subtypes like
 * AuthorizationDeniedException raised by @PreAuthorize) are NOT swallowed
 * by the catch-all RuntimeException handler. Pre-fix this regressed and
 * caused 15 controller tests to see 500 instead of 403.
 */
class GlobalErrorAdviceTest {

    private final GlobalErrorAdvice advice = new GlobalErrorAdvice();

    @Test
    void illegalArgumentMapsTo400_withMessage() {
        ResponseEntity<Map<String, Object>> r =
                advice.handleIllegalArgument(new IllegalArgumentException("bad input"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("error", "Bad Request");
        assertThat(r.getBody()).containsEntry("message", "bad input");
    }

    @Test
    void illegalArgumentWithNullMessage_doesNotNpe() {
        ResponseEntity<Map<String, Object>> r =
                advice.handleIllegalArgument(new IllegalArgumentException());

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("message", "Invalid argument");
    }

    @Test
    void accessDeniedMapsTo403_notSwallowedAs500() {
        // Repro for the bug: AccessDeniedException extends RuntimeException, so prior
        // to the explicit handler the catch-all converted it to 500. Tests that asserted
        // .isForbidden() then failed with 500 vs 403.
        ResponseEntity<Map<String, Object>> r =
                advice.handleAccessDenied(new AccessDeniedException("Access is denied"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r.getBody()).containsEntry("error", "Forbidden");
        assertThat(r.getBody()).containsEntry("message", "Access is denied");
    }

    @Test
    void authorizationDeniedExceptionAlsoMapsTo403_viaSubtypeMatch() {
        // AuthorizationDeniedException is what Spring Security 6's @PreAuthorize raises.
        // It extends AccessDeniedException, so the same handler catches it.
        AuthorizationDeniedException ade = new AuthorizationDeniedException(
                "Access Denied",
                new AuthorizationDecision(false));

        ResponseEntity<Map<String, Object>> r = advice.handleAccessDenied(ade);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r.getBody()).containsEntry("error", "Forbidden");
    }

    @Test
    void authenticationExceptionMapsTo401() {
        ResponseEntity<Map<String, Object>> r =
                advice.handleAuthentication(new BadCredentialsException("bad password"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody()).containsEntry("error", "Unauthorized");
        assertThat(r.getBody()).containsEntry("message", "bad password");
    }

    @Test
    void responseStatusException_isRethrown_notSwallowed() {
        // The catch-all must let ResponseStatusException pass through so that Spring's
        // own resolver maps it using the carried status.
        ResponseStatusException rse = new ResponseStatusException(HttpStatus.NOT_FOUND, "missing");

        assertThatThrownBy(() -> advice.handleRuntimeException(rse))
                .isSameAs(rse);
    }

    @Test
    void exceptionAnnotatedWithResponseStatus_isRethrown() {
        // Some exceptions in this codebase use @ResponseStatus on the class itself
        // (e.g. UnsupportedConMonFormatException → 400). The catch-all must re-throw
        // them so Spring's ResponseStatusExceptionResolver can map the status.
        @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CONFLICT)
        class TaggedException extends RuntimeException {}

        TaggedException tagged = new TaggedException();
        assertThatThrownBy(() -> advice.handleRuntimeException(tagged))
                .isSameAs(tagged);
    }

    @Test
    void unhandledRuntimeException_mapsTo500_withMessage() {
        ResponseEntity<Map<String, Object>> r =
                advice.handleRuntimeException(new RuntimeException("kaboom"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.getBody()).containsEntry("error", "Internal Server Error");
        assertThat(r.getBody()).containsEntry("message", "kaboom");
    }

    @Test
    void unhandledRuntimeException_withNullMessage_fallsBackToClassName() {
        ResponseEntity<Map<String, Object>> r =
                advice.handleRuntimeException(new IllegalStateException());

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.getBody()).containsEntry("message", "IllegalStateException");
    }
}
