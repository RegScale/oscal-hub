package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NoActiveOrganizationException extends RuntimeException {

    public NoActiveOrganizationException(String username) {
        super("User '" + username + "' has no active organization membership.");
    }
}
