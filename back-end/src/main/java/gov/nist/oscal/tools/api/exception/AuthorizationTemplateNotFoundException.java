package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AuthorizationTemplateNotFoundException extends RuntimeException {

    public AuthorizationTemplateNotFoundException(Long id) {
        super("Authorization template " + id + " not found.");
    }
}
