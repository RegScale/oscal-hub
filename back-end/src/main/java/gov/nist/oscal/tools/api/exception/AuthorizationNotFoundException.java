package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AuthorizationNotFoundException extends RuntimeException {

    public AuthorizationNotFoundException(Long id) {
        super("Authorization " + id + " not found.");
    }
}
