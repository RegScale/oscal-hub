package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class InsufficientAuthorizationRoleException extends RuntimeException {

    public InsufficientAuthorizationRoleException(String currentRole, String requiredRole) {
        super("Insufficient role: have " + currentRole + ", need " + requiredRole + ".");
    }
}
