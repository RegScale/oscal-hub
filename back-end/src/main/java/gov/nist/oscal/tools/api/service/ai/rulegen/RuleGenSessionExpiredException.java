package gov.nist.oscal.tools.api.service.ai.rulegen;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the rule-gen wizard session a request refers to is not in the
 * in-memory store — typically because the backend restarted (dev hot-reload,
 * deploy) or because the 30-minute access TTL elapsed.
 *
 * <p>Mapped to HTTP {@code 410 Gone} so the frontend can distinguish this
 * recoverable case from a generic {@code 500} and silently restart the
 * session before retrying the user's last message.
 */
@ResponseStatus(HttpStatus.GONE)
public class RuleGenSessionExpiredException extends RuntimeException {
    public RuleGenSessionExpiredException(String message) {
        super(message);
    }
}
